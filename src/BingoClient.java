/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * Cliente de Bingo com interface gráfica e comunicação por sockets
 */
public class BingoClient extends JFrame {
    private static final String SERVIDOR_ANFITRIAO = "localhost";
    private static final int SERVIDOR_PORTA = 12345;
    
    private JTextField campoNome;
    private JButton botaoPronto, botaoLinha, botaoBingo;
    private JLabel rotuloEstado, rotuloIdCartao;
    private JPanel painelCartao;
    private JButton[] botoesCartao = new JButton[25];
    
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private boolean ligado = false;
    
    private String idCartao;
    private int[] cartao = new int[25];
    private boolean jogoIniciado = false;
    
    public BingoClient() {
        inicializarInterface();
        ligarAoServidor();
    }
    
    private void inicializarInterface() {
        setTitle("Cliente Bingo ESTGA");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Painel superior
        JPanel painelSuperior = new JPanel(new BorderLayout());
        JPanel painelNome = new JPanel(new FlowLayout(FlowLayout.LEFT));
        campoNome = new JTextField(15);
        painelNome.add(new JLabel("Nome: "));
        painelNome.add(campoNome);
        painelSuperior.add(painelNome, BorderLayout.WEST);
        
        rotuloIdCartao = new JLabel("ID do Cartão: N/A");
        JPanel painelId = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelId.add(rotuloIdCartao);
        painelSuperior.add(painelId, BorderLayout.EAST);
        add(painelSuperior, BorderLayout.NORTH);
        
        // Painel central com cartão
        JPanel painelCentral = new JPanel(new BorderLayout());
        painelCartao = new JPanel(new GridLayout(5, 5, 5, 5));
        painelCartao.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (int i = 0; i < 25; i++) {
            botoesCartao[i] = new JButton(" ");
            botoesCartao[i].setFont(new Font("Arial", Font.BOLD, 20));
            botoesCartao[i].setEnabled(false);
            botoesCartao[i].setBackground(Color.WHITE);
            painelCartao.add(botoesCartao[i]);
        }
        painelCentral.add(painelCartao, BorderLayout.CENTER);
        
        // Botões de controlo
        JPanel painelBotoes = new JPanel(new FlowLayout());
        botaoPronto = new JButton("Pronto para iniciar");
        botaoLinha = new JButton("Linha");
        botaoBingo = new JButton("Bingo");
        
        botaoPronto.setEnabled(false);
        botaoLinha.setEnabled(false);
        botaoBingo.setEnabled(false);
        
        painelBotoes.add(botaoPronto);
        painelBotoes.add(botaoLinha);
        painelBotoes.add(botaoBingo);
        painelCentral.add(painelBotoes, BorderLayout.SOUTH);
        add(painelCentral, BorderLayout.CENTER);
        
        rotuloEstado = new JLabel("A ligar ao servidor...", SwingConstants.CENTER);
        add(rotuloEstado, BorderLayout.SOUTH);
        
        configurarEventos();
        setVisible(true);
    }
    
    private void configurarEventos() {
        botaoPronto.addActionListener(e -> {
            String nomeJogador = campoNome.getText().trim();
            if (nomeJogador.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduza o seu nome.");
            } else if (ligado) {
                enviarMensagem("PRONTO:" + nomeJogador);
                botaoPronto.setEnabled(false);
                campoNome.setEnabled(false);
            }
        });
    }
    
    private void ligarAoServidor() {
        try {
            socket = new Socket(SERVIDOR_ANFITRIAO, SERVIDOR_PORTA);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);
            ligado = true;
            rotuloEstado.setText("Ligado! Introduza o seu nome e clique em 'Pronto para iniciar'.");
            botaoPronto.setEnabled(true);
            
            new Thread(this::escutarServidor).start();
        } catch (IOException e) {
            rotuloEstado.setText("Erro ao ligar: " + e.getMessage());
        }
    }
    
    private void escutarServidor() {
        try {
            String mensagem;
            while (ligado && (mensagem = entrada.readLine()) != null) {
                System.out.println("Recebido: " + mensagem);
                // TODO: Processar mensagens do servidor
            }
        } catch (IOException e) {
            if (ligado) {
                SwingUtilities.invokeLater(() -> 
                    rotuloEstado.setText("Ligação perdida"));
            }
        }
    }
    
    private void enviarMensagem(String mensagem) {
        if (saida != null && ligado) {
            saida.println(mensagem);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BingoClient());
    }
}