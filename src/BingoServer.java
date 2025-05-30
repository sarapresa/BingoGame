/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
    private boolean jogoTerminado;
    
    public BingoServer() {
        clientes = Collections.synchronizedList(new ArrayList<>());
        jogoIniciado = false;
        jogoTerminado = false;
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
    public static void main(String[] args) {
        BingoServer servidor = new BingoServer();
        servidor.iniciar();
    }
}