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
 */
public class BingoServer {
    private static final int PORTA = 12345;
    private static final int MAX_JOGADORES = 10;
    private static final int MIN_JOGADORES = 2;
    
    private ServerSocket socketServidor;
    private List<ClientHandler> clientes;
    private boolean jogoIniciado;
    private boolean jogoTerminado;
    private static final int INTERVALO_SORTEIO = 10000; // 10 segundos
    private Set<Integer> numerosSorteados;
    private List<Integer> historicoPorNum;
    private Timer temporizadorSorteio;
    
    public BingoServer() {
        clientes = Collections.synchronizedList(new ArrayList<>());
        numerosSorteados = Collections.synchronizedSet(new HashSet<>());
        historicoPorNum = Collections.synchronizedList(new ArrayList<>());
        jogoIniciado = false;
        jogoTerminado = false;
        aleatorio = new Random();
    }
    
    private Random aleatorio = new Random();
    
    public synchronized int[] gerarCartao() {
        Set<Integer> numerosCartao = new LinkedHashSet<>();
        while (numerosCartao.size() < 25) {
            numerosCartao.add(aleatorio.nextInt(99) + 1);
        }
        return numerosCartao.stream().mapToInt(Integer::intValue).toArray();
    }
    
    public synchronized String gerarIdCartao() { 
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    public void iniciar() {
        try {
            socketServidor = new ServerSocket(PORTA);
            System.out.println("Servidor de Bingo iniciado na porta " + PORTA);
            System.out.println("À espera de jogadores... (mínimo: " + MIN_JOGADORES + ", máximo: " + MAX_JOGADORES + ")");
            
            while (!socketServidor.isClosed()) {
                try {
                    Socket socketCliente = socketServidor.accept();
                    System.out.println("Novo cliente ligado: " + socketCliente.getInetAddress());
                    
                    synchronized (this) {
                        if (clientes.size() >= MAX_JOGADORES) {
                            PrintWriter saida = new PrintWriter(socketCliente.getOutputStream(), true);
                            saida.println("ERRO:Servidor lotado. Tente novamente mais tarde.");
                            socketCliente.close();
                            continue;
                        }
                        
                        if (jogoIniciado) {
                            PrintWriter saida = new PrintWriter(socketCliente.getOutputStream(), true);
                            saida.println("ERRO:Jogo já em andamento. Tente novamente mais tarde.");
                            socketCliente.close();
                            continue;
                        }
                    }
                    
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
        }
    }
    
    public synchronized void registarCliente(ClientHandler cliente) {
        System.out.println("Cliente " + cliente.obterNome() + " registado. Total: " + clientes.size());
    }
    
    public synchronized void removerCliente(ClientHandler cliente) {
        clientes.remove(cliente);
        System.out.println("Cliente desligado. Total: " + clientes.size());
    }
    public synchronized void verificarTodosProntos() {
        if (jogoIniciado || jogoTerminado) {
            return;
        }
        
        if (clientes.size() < MIN_JOGADORES) {
            System.out.println("A aguardar mais jogadores. Atual: " + clientes.size() + "/" + MIN_JOGADORES);
            return;
        }
        
        int jogadoresProntos = 0;
        for (ClientHandler cliente : clientes) {
            if (cliente.estaPronto()) {
                jogadoresProntos++;
            }
        }
        
        System.out.println("Jogadores prontos: " + jogadoresProntos + "/" + clientes.size());
        
        if (jogadoresProntos >= MIN_JOGADORES && jogadoresProntos == clientes.size()) {
            iniciarJogo();
        }
    }
    
       private void iniciarJogo() {
        jogoIniciado = true;
        System.out.println("Todos os jogadores estão prontos! O jogo vai começar.");
        enviarMensagemTodos("JOGO_INICIADO:O jogo começou! Boa sorte!");
        
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
    
    private synchronized void sortearNumero() {
        if (jogoTerminado) return;
        
        if (numerosSorteados.size() >= 99) {
            enviarMensagemTodos("FIM_DE_JOGO:Todos os números foram sorteados.");
            terminarJogo("Todos os números sorteados");
            return;
        }
        
        int numeroSorteado;
        do {
            numeroSorteado = aleatorio.nextInt(99) + 1;
        } while (numerosSorteados.contains(numeroSorteado));
        
        numerosSorteados.add(numeroSorteado);
        historicoPorNum.add(numeroSorteado);
        
        System.out.println("Número sorteado: " + numeroSorteado);
        enviarMensagemTodos("NUMERO_SORTEADO:" + numeroSorteado);
    }
    
    public Set<Integer> obterNumerosSorteados() {
        return new HashSet<>(numerosSorteados);
    }
    
    
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
    
    
    public synchronized void enviarMensagemTodos(String mensagem) {
        List<ClientHandler> clientesCopia = new ArrayList<>(clientes);
        for (ClientHandler cliente : clientesCopia) {
            try {
                cliente.enviarMensagem(mensagem);
            } catch (Exception e) {
                System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            }
        }
    }
    
    private boolean validarBingo(int[] cartao, Set<Integer> numerosMarados, Set<Integer> numerosSorteadosNoJogo) {
    for (int numeroNoCartao : cartao) {
        // Verifica se TODOS os números estão marcados pelo jogador E foram sorteados
        if (!numerosMarados.contains(numeroNoCartao) || !numerosSorteadosNoJogo.contains(numeroNoCartao)) {
            return false;
        }
    }
    return true;
}
    private void terminarJogo(String razao) {
    jogoTerminado = true;
    if (temporizadorSorteio != null) {
        temporizadorSorteio.cancel();
        temporizadorSorteio.purge();
        temporizadorSorteio = null;
    }
    System.out.println("Jogo terminado: " + razao);
}
    
    public static void main(String[] args) {
        BingoServer servidor = new BingoServer();
        servidor.iniciar();
    }

    private void enviarMensagemParaOutros(String string, ClientHandler cliente) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}