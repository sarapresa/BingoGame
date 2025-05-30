/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author diogo
 */
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Cliente de Bingo com interface gráfica
 */
public class BingoClient extends JFrame {
    private static final String SERVIDOR_ANFITRIAO = "localhost";
    private static final int SERVIDOR_PORTA = 12345;
    
    private JTextField campoNome;
    private JButton botaoPronto;
    private JLabel rotuloEstado;
    private JPanel painelCartao;
    private JButton[] botoesCartao = new JButton[25];
    
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    
    public BingoClient() {
        inicializarInterface();
    }
    
    private void inicializarInterface() {
        setTitle("Cliente Bingo ESTGA");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Painel superior com nome
        JPanel painelNome = new JPanel(new FlowLayout());
        campoNome = new JTextField(15);
        botaoPronto = new JButton("Pronto para iniciar");
        painelNome.add(new JLabel("Nome: "));
        painelNome.add(campoNome);
        painelNome.add(botaoPronto);
        add(painelNome, BorderLayout.NORTH);
        
        // Cartão de bingo (5x5)
        painelCartao = new JPanel(new GridLayout(5, 5, 5, 5));
        for (int i = 0; i < 25; i++) {
            botoesCartao[i] = new JButton(" ");
            botoesCartao[i].setEnabled(false);
            painelCartao.add(botoesCartao[i]);
        }
        add(painelCartao, BorderLayout.CENTER);
        
        // Estado
        rotuloEstado = new JLabel("Pronto para ligar", SwingConstants.CENTER);
        add(rotuloEstado, BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BingoClient());
    }
}
