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
    
    private String idCartao;
    private int[] cartao;
    private Set<Integer> numerosMarados = new HashSet<>();
    
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
            if (nome.isEmpty()) {
                enviarMensagem("ERRO:Nome não pode estar vazio.");
                return;
            }
            
            pronto = true;
            idCartao = servidor.gerarIdCartao();
            cartao = servidor.gerarCartao();
            servidor.registarCliente(this);
            enviarCartao();
            servidor.verificarTodosProntos();
            System.out.println("Cliente " + nome + " está pronto com cartão " + idCartao);
        }
    }
        
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

private void desmarcarNumero(int numero) {
    numerosMarados.remove(numero);
    System.out.println("Jogador " + nome + " desmarcou número " + numero);
}
    
    private void enviarCartao() {
        StringBuilder sb = new StringBuilder("CARTAO:" + idCartao + ":");
        for (int i = 0; i < cartao.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(cartao[i]);
        }
        enviarMensagem(sb.toString());
        System.out.println("Cartão enviado para " + nome);
    }
    
    public int[] obterCartao() {
        return cartao != null ? cartao.clone() : new int[0];
    }
    
    public Set<Integer> obterNumerosMarados() {
        return new HashSet<>(numerosMarados);
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