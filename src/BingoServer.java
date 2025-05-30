/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servidor de Bingo que gere múltiplos clientes e coordena o jogo
 * Implementa as funcionalidades principais do jogo de bingo multijogador
 */
public class BingoServer {
    // Constantes de configuração do servidor
    private static final int PORTA = 12345;
    private static final int MAX_JOGADORES = 10;
    private static final int MIN_JOGADORES = 2;
    private static final int INTERVALO_SORTEIO = 10000; // 10 segundos conforme solicitado

    // Componentes principais do servidor
    private ServerSocket socketServidor;
    private List<ClientHandler> clientes;
    private Set<Integer> numerosSorteados;
    private List<Integer> historicoPorNum;
    private boolean jogoIniciado;
    private boolean jogoTerminado;
    private Timer temporizadorSorteio;
    private Random aleatorio;

    /**
     * Construtor - Inicializa todas as estruturas de dados do servidor
     */
    public BingoServer() {
        clientes = Collections.synchronizedList(new ArrayList<>());
        numerosSorteados = Collections.synchronizedSet(new HashSet<>());
        historicoPorNum = Collections.synchronizedList(new ArrayList<>());
        jogoIniciado = false;
        jogoTerminado = false;
        aleatorio = new Random();
    }

    /**
     * Retorna uma cópia dos números sorteados para evitar modificações externas
     */
    public Set<Integer> obterNumerosSorteados() {
        return new HashSet<>(numerosSorteados);
    }

    /**
     * Método principal que inicia o servidor e aceita ligações de clientes
     */
    public void iniciar() {
        try {
            socketServidor = new ServerSocket(PORTA);
            System.out.println("Servidor de Bingo iniciado na porta " + PORTA);
            System.out.println("À espera de jogadores... (mínimo: " + MIN_JOGADORES + ", máximo: " + MAX_JOGADORES + ")");

            // Loop principal para aceitar ligações
            while (!socketServidor.isClosed()) {
                try {
                    Socket socketCliente = socketServidor.accept();
                    System.out.println("Novo cliente ligado: " + socketCliente.getInetAddress());

                    // Verificações de segurança antes de aceitar o cliente
                    synchronized (this) {
                        // Verifica se o servidor não está lotado
                        if (clientes.size() >= MAX_JOGADORES) {
                            System.out.println("Servidor lotado. A rejeitar ligação de " + socketCliente.getInetAddress());
                            PrintWriter saida = new PrintWriter(socketCliente.getOutputStream(), true);
                            saida.println("ERRO:Servidor lotado. Tente novamente mais tarde.");
                            socketCliente.close();
                            continue;
                        }

                        // Verifica se o jogo já não começou
                        if (jogoIniciado) {
                            System.out.println("Jogo já iniciado. A rejeitar ligação de " + socketCliente.getInetAddress());
                            PrintWriter saida = new PrintWriter(socketCliente.getOutputStream(), true);
                            saida.println("ERRO:Jogo já em andamento. Tente novamente mais tarde.");
                            socketCliente.close();
                            continue;
                        }
                    }

                    // Cria e inicia um gestor para o novo cliente
                    ClientHandler gestorCliente = new ClientHandler(socketCliente, this);
                    synchronized (this) {
                        clientes.add(gestorCliente);
                    }
                    new Thread(gestorCliente).start();
                } catch (IOException e) {
                    if (!socketServidor.isClosed()) {
                        System.err.println("Erro ao aceitar ligação: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        } finally {
            pararServidor();
        }
    }

    /**
     * Gera um cartão de bingo com 25 números únicos entre 1 e 99
     */
    public synchronized int[] gerarCartao() {
        Set<Integer> numerosCartao = new LinkedHashSet<>();
        while (numerosCartao.size() < 25) {
            numerosCartao.add(aleatorio.nextInt(99) + 1); // Números de 1 a 99
        }
        return numerosCartao.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Gera um identificador único para cada cartão
     */
    public synchronized String gerarIdCartao() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Regista um cliente no servidor (já foi adicionado à lista anteriormente)
     */
    public synchronized void registarCliente(ClientHandler cliente) {
        System.out.println("Cliente " + cliente.obterNome() + " registado. Total de clientes: " + clientes.size());
    }

    /**
     * Verifica se todos os jogadores estão prontos para iniciar o jogo
     * Esta é uma das funções mais importantes do servidor
     */
    public synchronized void verificarTodosProntos() {
        // Não faz nada se o jogo já iniciou ou terminou
        if (jogoIniciado || jogoTerminado) {
            return;
        }

        // Verifica se temos jogadores suficientes
        if (clientes.size() < MIN_JOGADORES) {
            System.out.println("A aguardar mais jogadores. Atual: " + clientes.size() + "/" + MIN_JOGADORES);
            return;
        }

        // Conta quantos jogadores estão prontos
        int jogadoresProntos = 0;
        for (ClientHandler cliente : clientes) {
            if (cliente.estaPronto()) {
                jogadoresProntos++;
            }
        }

        System.out.println("Jogadores prontos: " + jogadoresProntos + "/" + clientes.size());

        // Inicia o jogo se todos estiverem prontos e temos o mínimo necessário
        if (jogadoresProntos >= MIN_JOGADORES && jogadoresProntos == clientes.size()) {
            iniciarJogo();
        }
    }

    /**
     * Inicia efetivamente o jogo de bingo
     * Configura o temporizador para sorteio automático dos números
     */
    private void iniciarJogo() {
        jogoIniciado = true;
        System.out.println("Todos os jogadores estão prontos! O jogo vai começar.");
        enviarMensagemTodos("JOGO_INICIADO:O jogo começou! Boa sorte!");

        // Configura o temporizador para sorteio automático
        // Primeira chamada após 5 segundos, depois a cada 10 segundos
        temporizadorSorteio = new Timer();
        temporizadorSorteio.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!jogoTerminado) {
                    sortearNumero();
                } else {
                    this.cancel();
                }
            }
        }, 5000, INTERVALO_SORTEIO);
    }

    /**
     * Sorteia um número aleatório que ainda não foi sorteado
     * Envia o número para todos os clientes ligados
     */
    private synchronized void sortearNumero() {
        if (jogoTerminado) return;

        // Verifica se todos os números já foram sorteados
        if (numerosSorteados.size() >= 99) {
            enviarMensagemTodos("FIM_DE_JOGO:Todos os números foram sorteados. Ninguém fez BINGO.");
            terminarJogo("Todos os números sorteados");
            return;
        }

        // Sorteia um número que ainda não foi sorteado
        int numeroSorteado;
        do {
            numeroSorteado = aleatorio.nextInt(99) + 1;
        } while (numerosSorteados.contains(numeroSorteado));

        // Regista o número sorteado
        numerosSorteados.add(numeroSorteado);
        historicoPorNum.add(numeroSorteado);
        
        System.out.println("Número sorteado: " + numeroSorteado + " (Total sorteados: " + numerosSorteados.size() + ")");
        enviarMensagemTodos("NUMERO_SORTEADO:" + numeroSorteado);
    }

    /**
     * Processa um pedido de linha de um jogador
     * Valida se o jogador realmente completou uma linha
     */
    public synchronized void processarLinha(ClientHandler cliente) {
        // Verificações básicas de estado do jogo
        if (jogoTerminado) {
            cliente.enviarMensagem("ERRO:O jogo já terminou.");
            return;
        }

        if (!jogoIniciado) {
            cliente.enviarMensagem("ERRO:O jogo ainda não começou.");
            return;
        }

        System.out.println("A processar pedido de LINHA de " + cliente.obterNome());
        
        // Valida a linha do jogador
        if (validarLinha(cliente.obterCartao(), cliente.obterNumerosMarados(), numerosSorteados)) {
            System.out.println("LINHA VÁLIDA para " + cliente.obterNome());
            enviarMensagemTodos("LINHA_VALIDA:" + cliente.obterNome());
        } else {
            System.out.println("LINHA INVÁLIDA para " + cliente.obterNome());
            cliente.enviarMensagem("LINHA_INVALIDA");
        }
    }

    /**
     * Processa um pedido de bingo de um jogador
     * Valida se o jogador completou todo o cartão
     */
    public synchronized void processarBingo(ClientHandler cliente) {
        // Verificações básicas de estado do jogo
        if (jogoTerminado) {
            cliente.enviarMensagem("ERRO:O jogo já terminou.");
            return;
        }

        if (!jogoIniciado) {
            cliente.enviarMensagem("ERRO:O jogo ainda não começou.");
            return;
        }

        System.out.println("A processar pedido de BINGO de " + cliente.obterNome());
        
        // Valida o bingo do jogador
        if (validarBingo(cliente.obterCartao(), cliente.obterNumerosMarados(), numerosSorteados)) {
            System.out.println("BINGO VÁLIDO para " + cliente.obterNome());
            cliente.enviarMensagem("BINGO_VALIDO");
            enviarMensagemParaOutros("BINGO_OUTROS:" + cliente.obterNome(), cliente);
            terminarJogo("Bingo feito por " + cliente.obterNome());
        } else {
            System.out.println("BINGO INVÁLIDO para " + cliente.obterNome());
            cliente.enviarMensagem("BINGO_INVALIDO");
        }
    }

    /**
     * Valida se o jogador fez uma linha válida
     * Verifica linhas horizontais e verticais
     * Todos os números da linha devem estar marcados E ter sido sorteados
     */
    private boolean validarLinha(int[] cartao, Set<Integer> numerosMarados, Set<Integer> numerosSorteadosNoJogo) {
        // Verifica linhas horizontais (5 linhas de 5 números cada)
        for (int linha = 0; linha < 5; linha++) {
            boolean linhaCompleta = true;
            for (int coluna = 0; coluna < 5; coluna++) {
                int indice = linha * 5 + coluna;
                int numeroNoCartao = cartao[indice];
                
                // Verifica se o número está marcado pelo jogador E foi sorteado
                if (!numerosMarados.contains(numeroNoCartao) || !numerosSorteadosNoJogo.contains(numeroNoCartao)) {
                    linhaCompleta = false;
                    break;
                }
            }
            if (linhaCompleta) {
                System.out.println("Linha horizontal " + (linha + 1) + " completa!");
                return true;
            }
        }

        // Verifica linhas verticais (5 colunas de 5 números cada)
        for (int coluna = 0; coluna < 5; coluna++) {
            boolean colunaCompleta = true;
            for (int linha = 0; linha < 5; linha++) {
                int indice = linha * 5 + coluna;
                int numeroNoCartao = cartao[indice];
                
                // Verifica se o número está marcado pelo jogador E foi sorteado
                if (!numerosMarados.contains(numeroNoCartao) || !numerosSorteadosNoJogo.contains(numeroNoCartao)) {
                    colunaCompleta = false;
                    break;
                }
            }
            if (colunaCompleta) {
                System.out.println("Linha vertical " + (coluna + 1) + " completa!");
                return true;
            }
        }

        return false;
    }

    /**
     * Valida se o jogador fez bingo válido
     * TODOS os 25 números do cartão devem estar marcados E ter sido sorteados
     */
    private boolean validarBingo(int[] cartao, Set<Integer> numerosMarados, Set<Integer> numerosSorteadosNoJogo) {
        for (int numeroNoCartao : cartao) {
            // Verifica se TODOS os números estão marcados pelo jogador E foram sorteados
            if (!numerosMarados.contains(numeroNoCartao) || !numerosSorteadosNoJogo.contains(numeroNoCartao)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Envia uma mensagem para todos os clientes ligados
     */
    public synchronized void enviarMensagemTodos(String mensagem) {
        List<ClientHandler> clientesCopia = new ArrayList<>(clientes);
        for (ClientHandler cliente : clientesCopia) {
            try {
                cliente.enviarMensagem(mensagem);
            } catch (Exception e) {
                System.err.println("Erro ao enviar mensagem para cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Envia uma mensagem para todos os clientes exceto um específico
     */
    public synchronized void enviarMensagemParaOutros(String mensagem, ClientHandler excluir) {
        List<ClientHandler> clientesCopia = new ArrayList<>(clientes);
        for (ClientHandler cliente : clientesCopia) {
            if (cliente != excluir) {
                try {
                    cliente.enviarMensagem(mensagem);
                } catch (Exception e) {
                    System.err.println("Erro ao enviar mensagem para cliente: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Remove um cliente da lista quando se desliga
     */
    public synchronized void removerCliente(ClientHandler cliente) {
        clientes.remove(cliente);
        System.out.println("Cliente " + (cliente.obterNome() != null ? cliente.obterNome() : "desconhecido") + 
                          " desligado. Total: " + clientes.size());
        
        // Se o jogo não começou e ficamos sem jogadores suficientes, reinicia o estado
        if (!jogoIniciado && clientes.size() < MIN_JOGADORES) {
            System.out.println("Não há jogadores suficientes. A aguardar mais ligações...");
        }
    }

    /**
     * Termina o jogo e limpa os recursos utilizados
     */
    private void terminarJogo(String razao) {
        jogoTerminado = true;
        if (temporizadorSorteio != null) {
            temporizadorSorteio.cancel();
            temporizadorSorteio.purge();
            temporizadorSorteio = null;
        }
        System.out.println("Jogo terminado: " + razao);
    }

    /**
     * Para o servidor de forma controlada, desligando todos os clientes
     */
    public void pararServidor() {
        System.out.println("A parar servidor...");
        try {
            if (temporizadorSorteio != null) {
                temporizadorSorteio.cancel();
                temporizadorSorteio.purge();
            }
            
            // Informa todos os clientes que o servidor está a parar
            synchronized (this) {
                for (ClientHandler cliente : new ArrayList<>(clientes)) {
                    try {
                        cliente.enviarMensagem("ERRO:Servidor está a parar. A desligar...");
                        cliente.fecharLigacaoDoServidor();
                    } catch (Exception e) {
                        System.err.println("Erro ao desligar cliente: " + e.getMessage());
                    }
                }
                clientes.clear();
            }
            
            if (socketServidor != null && !socketServidor.isClosed()) {
                socketServidor.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao parar servidor: " + e.getMessage());
        }
        System.out.println("Servidor parado.");
    }

    /**
     * Método principal - ponto de entrada da aplicação
     */
    public static void main(String[] args) {
        BingoServer servidor = new BingoServer();
        
        // Adiciona um hook para parar o servidor de forma elegante quando o programa termina
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nRecebido sinal de paragem...");
            servidor.pararServidor();
        }));
        
        servidor.iniciar();
    }
}