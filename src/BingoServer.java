import java.io.*;
import java.net.*;
import java.util.*;

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
    
    public BingoServer() {
        clientes = new ArrayList<>();
        jogoIniciado = false;
    }
    
    public void iniciar() {
        try {
            socketServidor = new ServerSocket(PORTA);
            System.out.println("Servidor de Bingo iniciado na porta " + PORTA);
            
            while (!socketServidor.isClosed()) {
                Socket socketCliente = socketServidor.accept();
                System.out.println("Novo cliente ligado: " + socketCliente.getInetAddress());
                // TODO: Implementar gestão de clientes
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        BingoServer servidor = new BingoServer();
        servidor.iniciar();
    }
}