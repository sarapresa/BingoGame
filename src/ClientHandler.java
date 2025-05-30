/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe que gere cada cliente ligado ao servidor
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BingoServer servidor;
    private BufferedReader entrada;
    private PrintWriter saida;
    
    private String nome;
    private boolean pronto;
    private boolean ligado;
    
    public ClientHandler(Socket socket, BingoServer servidor) {
        this.socket = socket;
        this.servidor = servidor;
        this.pronto = false;
        this.ligado = true;
        
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erro ao inicializar streams: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            String mensagem;
            while (ligado && (mensagem = entrada.readLine()) != null) {
                System.out.println("Recebido: " + mensagem);
                processarMensagem(mensagem);
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação: " + e.getMessage());
        }
    }
    
    private void processarMensagem(String mensagem) {
        if (mensagem.startsWith("PRONTO:")) {
            nome = mensagem.substring("PRONTO:".length()).trim();
            pronto = true;
            System.out.println("Cliente " + nome + " está pronto");
        }
    }
    
    public void enviarMensagem(String mensagem) {
        if (saida != null && ligado) {
            saida.println(mensagem);
        }
    }
    
    public boolean estaPronto() {
        return pronto;
    }
    
    public String obterNome() {
        return nome != null ? nome : "Cliente Desconhecido";
    }
}