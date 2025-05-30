import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe que gere cada cliente ligado ao servidor
 * Cada instância desta classe representa um jogador no jogo de bingo
 * Processa as mensagens recebidas do cliente e mantém o estado do jogador
 */
public class ClientHandler implements Runnable {
    // Componentes de comunicação
    private Socket socket;
    private BingoServer servidor;
    private BufferedReader entrada;
    private PrintWriter saida;

    // Estado do jogador
    private String nome;
    private String idCartao;
    private int[] cartao;
    private boolean pronto;
    private boolean ligado;
    private Set<Integer> numerosMarados; // Números que o jogador marcou no seu cartão

    /**
     * Construtor - Inicializa a ligação com um cliente
     * @param socket - Socket de comunicação com o cliente
     * @param servidor - Referência para o servidor principal
     */
    public ClientHandler(Socket socket, BingoServer servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.pronto = false;
        this.ligado = true;
        this.numerosMarados = new HashSet<>();

        try {
            // Configura os streams de entrada e saída
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar streams para cliente: " + e.getMessage());
            fecharLigacao();
        }
    }

    /**
     * Método principal da thread - fica à escuta de mensagens do cliente
     * Implementa o protocolo de comunicação com o cliente
     */
    @Override
    public void run() {
        try {
            String mensagem;
            // Loop principal para receber mensagens
            while (ligado && (mensagem = entrada.readLine()) != null) {
                System.out.println("Recebido de " + (nome != null ? nome : "cliente") + ": " + mensagem);
                processarMensagem(mensagem);
            }
        } catch (IOException e) {
            if (ligado) {
                System.err.println("Erro na comunicação com cliente " + 
                                 (nome != null ? nome : "desconhecido") + ": " + e.getMessage());
            }
        } finally {
            fecharLigacao();
        }
    }

    /**
     * Processa as diferentes mensagens recebidas do cliente
     * Implementa o protocolo de comunicação definido
     * @param mensagem - Mensagem recebida do cliente
     */
    private void processarMensagem(String mensagem) {
        if (mensagem.startsWith("PRONTO:")) {
            // Cliente indica que está pronto com o seu nome
            // Formato: PRONTO:NomeDoJogador
            nome = mensagem.substring("PRONTO:".length()).trim();
            if (nome.isEmpty()) {
                enviarMensagem("ERRO:Nome não pode estar vazio.");
                return;
            }
            
            // Marca o jogador como pronto e gera o seu cartão
            pronto = true;
            idCartao = servidor.gerarIdCartao();
            cartao = servidor.gerarCartao();
            servidor.registarCliente(this);
            enviarCartao();
            servidor.verificarTodosProntos();
            
        } else if (mensagem.equals("LINHA")) {
            // Cliente reclama uma linha
            if (!pronto) {
                enviarMensagem("ERRO:Precisa estar pronto para jogar.");
                return;
            }
            servidor.processarLinha(this);
            
        } else if (mensagem.equals("BINGO")) {
            // Cliente reclama bingo (cartão completo)
            if (!pronto) {
                enviarMensagem("ERRO:Precisa estar pronto para jogar.");
                return;
            }
            servidor.processarBingo(this);
            
        } else if (mensagem.startsWith("MARCAR:")) {
            // Cliente marca um número no seu cartão
            // Formato: MARCAR:número
            try {
                int numero = Integer.parseInt(mensagem.substring("MARCAR:".length()));
                marcarNumero(numero);
            } catch (NumberFormatException e) {
                enviarMensagem("ERRO:Número inválido para marcar.");
            }
            
        } else if (mensagem.startsWith("DESMARCAR:")) {
            // Cliente desmarca um número no seu cartão
            // Formato: DESMARCAR:número
            try {
                int numero = Integer.parseInt(mensagem.substring("DESMARCAR:".length()));
                desmarcarNumero(numero);
            } catch (NumberFormatException e) {
                enviarMensagem("ERRO:Número inválido para desmarcar.");
            }
        } else {
            System.out.println("Mensagem desconhecida de " + (nome != null ? nome : "cliente") + ": " + mensagem);
        }
    }

    /**
     * Marca um número no cartão do jogador
     * Verifica se o número existe no cartão e se já foi sorteado
     * @param numero - Número a marcar
     */
    private void marcarNumero(int numero) {
        // Verifica se o número está no cartão do jogador
        boolean numeroNoCartao = false;
        for (int num : cartao) {
            if (num == numero) {
                numeroNoCartao = true;
                break;
            }
        }
        
        if (!numeroNoCartao) {
            enviarMensagem("ERRO:Número " + numero + " não está no seu cartão.");
            return;
        }
        
        // Verifica se o número foi sorteado pelo servidor
        if (!servidor.obterNumerosSorteados().contains(numero)) {
            enviarMensagem("ERRO:Número " + numero + " ainda não foi sorteado.");
            return;
        }
        
        // Marca o número
        numerosMarados.add(numero);
        System.out.println("Jogador " + nome + " marcou número " + numero);
    }

    /**
     * Desmarca um número no cartão do jogador
     * @param numero - Número a desmarcar
     */
    private void desmarcarNumero(int numero) {
        numerosMarados.remove(numero);
        System.out.println("Jogador " + nome + " desmarcou número " + numero);
    }

    /**
     * Envia o cartão gerado para o cliente
     * Formato: CARTAO:idCartao:num1,num2,num3,...,num25
     */
    private void enviarCartao() {
        StringBuilder sb = new StringBuilder("CARTAO:" + idCartao + ":");
        for (int i = 0; i < cartao.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(cartao[i]);
        }
        enviarMensagem(sb.toString());
        System.out.println("Cartão enviado para " + nome + " (ID: " + idCartao + ")");
    }

    /**
     * Envia uma mensagem para o cliente através do socket
     * @param mensagem - Mensagem a enviar
     */
    public void enviarMensagem(String mensagem) {
        if (saida != null && ligado) {
            saida.println(mensagem);
            System.out.println("Enviado para " + (nome != null ? nome : "cliente") + ": " + mensagem);
        }
    }

    // Métodos de acesso (getters) com nomes em português

    /**
     * Verifica se o jogador está pronto para começar o jogo
     */
    public boolean estaPronto() {
        return pronto;
    }

    /**
     * Obtém o nome do jogador
     */
    public String obterNome() {
        return nome != null ? nome : "Cliente Desconhecido";
    }

    /**
     * Obtém uma cópia do cartão do jogador (para segurança)
     */
    public int[] obterCartao() {
        return cartao != null ? cartao.clone() : new int[0];
    }

    /**
     * Obtém uma cópia dos números marcados pelo jogador (para segurança)
     */
    public Set<Integer> obterNumerosMarados() {
        return new HashSet<>(numerosMarados);
    }

    /**
     * Fecha a ligação quando chamado pelo servidor
     * Usado quando o servidor precisa desligar um cliente
     */
    public void fecharLigacaoDoServidor() {
        ligado = false;
        fecharLigacao();
    }

    /**
     * Fecha todos os recursos de comunicação de forma segura
     * Informa o servidor que este cliente se desligou
     */
    private void fecharLigacao() {
        ligado = false;
        try {
            // Fecha todos os streams e o socket
            if (entrada != null) entrada.close();
            if (saida != null) saida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar ligação com " + 
                             (nome != null ? nome : "cliente") + ": " + e.getMessage());
        } finally {
            // Remove este cliente do servidor
            servidor.removerCliente(this);
        }
    }
}