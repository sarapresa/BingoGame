/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.util.*;
import java.util.List;
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
    
    private JPanel painelNumerosSorteados;
    private List<JLabel> rotulosNumerosSorteados = new ArrayList<>();
    private Set<Integer> numerosSorteados = new HashSet<>(); // Para controlar números já sorteados
    private JScrollPane painelDeslizante;
    
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
        painelSuperior.add(painelId, BorderLayout.NORTH); // Adicionar o painelId ao painelSuperior
        add(painelSuperior, BorderLayout.NORTH); // Adicionar painelSuperior ao JFrame
        
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
        
        // Painel lateral com números sorteados (NOVO)
        painelNumerosSorteados = new JPanel();
        painelNumerosSorteados.setLayout(new BoxLayout(painelNumerosSorteados, BoxLayout.Y_AXIS));
        painelNumerosSorteados.setBorder(BorderFactory.createTitledBorder("Números Sorteados"));
        painelDeslizante = new JScrollPane(painelNumerosSorteados);
        painelDeslizante.setPreferredSize(new Dimension(150, 0)); // Largura preferencial
        add(painelDeslizante, BorderLayout.EAST); // Adiciona ao lado direito do JFrame
        
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
                processarMensagemServidor(mensagem);
            }
        } catch (IOException e) {
            if (ligado) {
                SwingUtilities.invokeLater(() -> 
                    rotuloEstado.setText("Ligação perdida"));
            }
        }
    }
    
    private void processarMensagemServidor(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            if (mensagem.startsWith("CARTAO:")) {
                processarCartaoRecebido(mensagem);
            } else if (mensagem.startsWith("JOGO_INICIADO:")) {
                iniciarJogo();
            } else if (mensagem.startsWith("NUMERO_SORTEADO:")) {
                processarNumeroSorteado(mensagem);
            } else if (mensagem.startsWith("ERRO:")) {
                String erro = mensagem.substring("ERRO:".length());
                rotuloEstado.setText("Erro: " + erro);
                JOptionPane.showMessageDialog(this, erro, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private void iniciarJogo() {
        jogoIniciado = true;
        botaoLinha.setEnabled(true);
        botaoBingo.setEnabled(true);
        // O campoNome deve ser desativado uma vez que o jogo começa ou que o jogador está pronto
        campoNome.setEnabled(false); 
        rotuloEstado.setText("JOGO INICIADO! Boa sorte!");
    }
    
    private void processarNumeroSorteado(String mensagem) {
        try {
            int numero = Integer.parseInt(mensagem.substring("NUMERO_SORTEADO:".length()).trim());
            numerosSorteados.add(numero);
            adicionarNumeroSorteado(numero);
            rotuloEstado.setText("Último número sorteado: " + numero);
            
            // Opcional: Destacar número no cartão se ele existir lá e não estiver marcado
            for (int i = 0; i < cartao.length; i++) {
                if (cartao[i] == numero) {
                    if (!numerosMarcados.contains(numero)) { // Apenas se não tiver sido marcado ainda
                         // Pode-se mudar a cor temporariamente ou adicionar um efeito
                         // Por agora, apenas atualizamos o estado, a marcação é manual pelo jogador
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Erro ao processar número sorteado: " + mensagem);
        }
    }
    
    private void adicionarNumeroSorteado(int numero) {
        JLabel rotuloNumero = new JLabel(String.valueOf(numero));
        rotuloNumero.setFont(new Font("Arial", Font.BOLD, 16));
        rotuloNumero.setAlignmentX(Component.CENTER_ALIGNMENT); // Centrar o texto
        painelNumerosSorteados.add(rotuloNumero);
        rotulosNumerosSorteados.add(rotuloNumero); // Adiciona à lista para fácil referência/limpeza
        painelNumerosSorteados.revalidate(); // Revalidar o painel para atualizar a UI
        painelNumerosSorteados.repaint(); // Repintar o painel
        
        // Rola automaticamente para ver o último número sorteado
        JScrollBar vertical = painelDeslizante.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    
    private void processarCartaoRecebido(String mensagem) {
        String[] partes = mensagem.split(":", 3);
        if (partes.length >= 3) {
            idCartao = partes[1];
            String[] numeros = partes[2].split(",");
            
            if (numeros.length == 25) {
                for (int i = 0; i < 25; i++) {
                    try {
                        cartao[i] = Integer.parseInt(numeros[i].trim());
                        botoesCartao[i].setText(String.valueOf(cartao[i]));
                        botoesCartao[i].setEnabled(true);
                    } catch (NumberFormatException e) {
                        System.err.println("Erro ao analisar número: " + numeros[i]);
                    }
                }
                rotuloIdCartao.setText("ID do Cartão: " + idCartao);
                rotuloEstado.setText("Cartão recebido! A aguardar outros jogadores...");
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