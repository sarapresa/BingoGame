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
import java.util.*;
import java.util.List;

/**
 * Cliente de Bingo com interface gráfica e comunicação por sockets
 * Esta classe representa a aplicação cliente que os jogadores utilizam para jogar Bingo
 * 
 */
public class BingoClient extends JFrame {
    // Configurações de ligação ao servidor
    private static final String SERVIDOR_ANFITRIAO = "localhost";
    private static final int SERVIDOR_PORTA = 12345;

    // Componentes da interface gráfica
    private JTextField campoNome;
    private JLabel rotuloNome;
    private JButton botaoPronto, botaoLinha, botaoBingo;
    private JLabel rotuloEstado, rotuloIdCartao;
    private JPanel painelCartao, painelNumerosSorteados;
    private JButton[] botoesCartao = new JButton[25];
    private List<JLabel> rotulosNumerosSorteados = new ArrayList<>();
    private JPanel painelSuperior;
    private JPanel painelNome;
    private JScrollPane painelDeslizante;

    // Componentes de comunicação de rede
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter saida;
    private boolean ligado = false;

    // Estado do jogo
    private String idCartao;
    private int[] cartao = new int[25];
    private Set<Integer> numerosMarcados = new HashSet<>();
    private Set<Integer> numerosSorteados = new HashSet<>();
    private boolean jogoIniciado = false;

    /**
     * Construtor principal - inicializa a interface e liga ao servidor
     */
    public BingoClient() {
        inicializarInterface();
        ligarAoServidor();
    }

    /**
     * Inicializa todos os componentes da interface gráfica
     * Cria o layout principal com cartão, botões e área de números sorteados
     */
    private void inicializarInterface() {
        setTitle("Cliente Bingo ESTGA");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Painel superior com nome do jogador e ID do cartão
        painelSuperior = new JPanel(new BorderLayout());
        painelNome = new JPanel(new FlowLayout(FlowLayout.LEFT));
        campoNome = new JTextField(15);
        painelNome.add(new JLabel("Nome: "));
        painelNome.add(campoNome);
        painelSuperior.add(painelNome, BorderLayout.WEST);

        rotuloIdCartao = new JLabel("ID do Cartão: N/A");
        JPanel painelId = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelId.add(rotuloIdCartao);
        painelSuperior.add(painelId, BorderLayout.EAST);
        add(painelSuperior, BorderLayout.NORTH);

        // Painel central com cartão de bingo (5x5) e botões de controlo
        JPanel painelCentral = new JPanel(new BorderLayout());
        painelCartao = new JPanel(new GridLayout(5, 5, 5, 5));
        painelCartao.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Criação dos 25 botões do cartão de bingo
        for (int i = 0; i < 25; i++) {
            botoesCartao[i] = new JButton(" ");
            botoesCartao[i].setFont(new Font("Arial", Font.BOLD, 20));
            botoesCartao[i].setEnabled(false);
            botoesCartao[i].setBackground(Color.WHITE);
            botoesCartao[i].setOpaque(true);
            botoesCartao[i].setBorderPainted(true);
            painelCartao.add(botoesCartao[i]);
            
            final int indice = i;
            botoesCartao[i].addActionListener(e -> alternarMarcacaoNumero(indice));
        }
        painelCentral.add(painelCartao, BorderLayout.CENTER);

        // Painel com botões de controlo do jogo
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
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

        // Painel lateral com histórico de números sorteados
        painelNumerosSorteados = new JPanel();
        painelNumerosSorteados.setLayout(new BoxLayout(painelNumerosSorteados, BoxLayout.Y_AXIS));
        painelNumerosSorteados.setBorder(BorderFactory.createTitledBorder("Números Sorteados"));
        painelDeslizante = new JScrollPane(painelNumerosSorteados);
        painelDeslizante.setPreferredSize(new Dimension(150, 0));
        painelDeslizante.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        painelDeslizante.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(painelDeslizante, BorderLayout.EAST);

        // Rótulo de estado na parte inferior
        rotuloEstado = new JLabel("A ligar ao servidor...", SwingConstants.CENTER);
        rotuloEstado.setFont(new Font("Arial", Font.BOLD, 14));
        rotuloEstado.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(rotuloEstado, BorderLayout.SOUTH);

        configurarEventos();
        setVisible(true);
    }

    /**
     * Configura todos os eventos dos botões e campos de texto
     */
    private void configurarEventos() {
        // Evento do botão "Pronto para iniciar"
        botaoPronto.addActionListener(e -> {
            String nomeJogador = campoNome.getText().trim();
            if (nomeJogador.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduza o seu nome.", "Erro", JOptionPane.ERROR_MESSAGE);
            } else if (!ligado) {
                JOptionPane.showMessageDialog(this, "Não ligado ao servidor.", "Erro", JOptionPane.ERROR_MESSAGE);
            } else {
                enviarMensagem("PRONTO:" + nomeJogador);
                botaoPronto.setEnabled(false);
                campoNome.setEnabled(false);
                rotuloEstado.setText("A aguardar outros jogadores...");
            }
        });

        // Evento do botão "Linha"
        botaoLinha.addActionListener(e -> {
            if (jogoIniciado) {
                enviarMensagem("LINHA");
            } else {
                rotuloEstado.setText("Aguarde o jogo começar!");
            }
        });

        // Evento do botão "Bingo"
        botaoBingo.addActionListener(e -> {
            if (jogoIniciado) {
                enviarMensagem("BINGO");
            } else {
                rotuloEstado.setText("Aguarde o jogo começar!");
            }
        });

        // Validação do campo nome para activar o botão "Pronto"
        campoNome.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarBotaoPronto(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarBotaoPronto(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarBotaoPronto(); }
            private void actualizarBotaoPronto() {
                botaoPronto.setEnabled(!campoNome.getText().trim().isEmpty() && ligado);
            }
        });
    }

    /**
     * Estabelece ligação TCP com o servidor de Bingo
     * Inicia thread separada para escutar mensagens do servidor
     */
    private void ligarAoServidor() {
        try {
            socket = new Socket(SERVIDOR_ANFITRIAO, SERVIDOR_PORTA);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            saida = new PrintWriter(socket.getOutputStream(), true);
            ligado = true;
            rotuloEstado.setText("Ligado! Introduza o seu nome e clique em 'Pronto para iniciar'.");
            botaoPronto.setEnabled(!campoNome.getText().trim().isEmpty());

            // Thread para escutar mensagens do servidor
            new Thread(this::escutarServidor).start();
        } catch (IOException e) {
            rotuloEstado.setText("Erro ao ligar: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Erro ao ligar ao servidor: " + e.getMessage(), 
                                        "Erro de Ligação", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Thread que escuta continuamente mensagens do servidor
     * Processa cada mensagem recebida
     */
    private void escutarServidor() {
        try {
            String mensagem;
            while (ligado && (mensagem = entrada.readLine()) != null) {
                System.out.println("Recebido do servidor: " + mensagem);
                processarMensagemServidor(mensagem);
            }
        } catch (IOException e) {
            if (ligado) {
                SwingUtilities.invokeLater(() -> {
                    rotuloEstado.setText("Ligação perdida com o servidor");
                    JOptionPane.showMessageDialog(this, "Ligação perdida com o servidor!", 
                                                "Erro", JOptionPane.ERROR_MESSAGE);
                });
            }
        }
    }

    /**
     * Processa as diferentes mensagens recebidas do servidor
     * Actualiza a interface gráfica conforme o tipo de mensagem
     */
    private void processarMensagemServidor(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            if (mensagem.startsWith("CARTAO:")) {
                processarCartaoRecebido(mensagem);
            } else if (mensagem.startsWith("JOGO_INICIADO:")) {
                iniciarJogo();
            } else if (mensagem.startsWith("NUMERO_SORTEADO:")) {
                processarNumeroSorteado(mensagem);
            } else if (mensagem.startsWith("LINHA_VALIDA:")) {
                processarLinhaValida(mensagem);
            } else if (mensagem.equals("LINHA_INVALIDA")) {
                processarLinhaInvalida();
            } else if (mensagem.equals("BINGO_VALIDO")) {
                processarBingoValido();
            } else if (mensagem.startsWith("BINGO_OUTROS:")) {
                processarBingoOutros(mensagem);
            } else if (mensagem.equals("BINGO_INVALIDO")) {
                processarBingoInvalido();
            } else if (mensagem.startsWith("FIM_DE_JOGO:")) {
                processarFimDeJogo(mensagem);
            } else if (mensagem.startsWith("ERRO:")) {
                processarErro(mensagem);
            }
        });
    }

    /**
     * Processa o cartão recebido do servidor
     * Actualiza a interface com os números do cartão
     */
    private void processarCartaoRecebido(String mensagem) {
        // Formato: CARTAO:ID:num1,num2,num3,...
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
                        System.err.println("Erro ao analisar número do cartão: " + numeros[i]);
                    }
                }
                rotuloIdCartao.setText("ID do Cartão: " + idCartao);
                rotuloEstado.setText("Cartão recebido! A aguardar outros jogadores...");
            }
        }
    }

    /**
     * Inicia o jogo quando todos os jogadores estão prontos
     */
    private void iniciarJogo() {
        jogoIniciado = true;
        botaoLinha.setEnabled(true);
        botaoBingo.setEnabled(true);
        rotuloEstado.setText("JOGO INICIADO! Boa sorte!");
    }

    /**
     * Processa um número sorteado pelo servidor
     * Adiciona à lista visual e destaca no cartão se existir
     */
    private void processarNumeroSorteado(String mensagem) {
        try {
            int numero = Integer.parseInt(mensagem.substring("NUMERO_SORTEADO:".length()));
            numerosSorteados.add(numero);
            adicionarNumeroSorteado(numero);
            destacarSeNoCartao(numero);
            rotuloEstado.setText("Último número sorteado: " + numero);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao analisar número sorteado: " + mensagem);
        }
    }

    /**
     * Processa linha válida de outro jogador
     */
    private void processarLinhaValida(String mensagem) {
        String jogador = mensagem.substring("LINHA_VALIDA:".length());
        rotuloEstado.setText("LINHA VÁLIDA feita por: " + jogador);
        JOptionPane.showMessageDialog(this, jogador + " fez uma linha válida!", 
                                    "Linha!", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Processa linha inválida do próprio jogador
     */
    private void processarLinhaInvalida() {
        rotuloEstado.setText("Linha inválida! Continue a jogar.");
        JOptionPane.showMessageDialog(this, "A sua linha não é válida. Continue a jogar!", 
                                    "Linha Inválida", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Processa bingo válido do próprio jogador (vitória)
     */
    private void processarBingoValido() {
        rotuloEstado.setText("PARABÉNS! Ganhou o BINGO!");
        JOptionPane.showMessageDialog(this, "PARABÉNS! Fez BINGO e ganhou o jogo!", 
                                    "BINGO!", JOptionPane.INFORMATION_MESSAGE);
        desactivarBotoes();
    }

    /**
     * Processa bingo válido de outro jogador
     */
    private void processarBingoOutros(String mensagem) {
        String vencedor = mensagem.substring("BINGO_OUTROS:".length());
        rotuloEstado.setText("Jogo terminado. Vencedor: " + vencedor);
        JOptionPane.showMessageDialog(this, vencedor + " fez BINGO e ganhou o jogo!", 
                                    "Jogo Terminado", JOptionPane.INFORMATION_MESSAGE);
        desactivarBotoes();
    }

    /**
     * Processa bingo inválido do próprio jogador
     */
    private void processarBingoInvalido() {
        rotuloEstado.setText("BINGO inválido! Continue a jogar.");
        JOptionPane.showMessageDialog(this, "O seu BINGO não é válido. Continue a jogar!", 
                                    "BINGO Inválido", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Processa fim de jogo por outras razões
     */
    private void processarFimDeJogo(String mensagem) {
        String razao = mensagem.substring("FIM_DE_JOGO:".length());
        rotuloEstado.setText("Jogo terminado: " + razao);
        JOptionPane.showMessageDialog(this, "Jogo terminado: " + razao, 
                                    "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
        desactivarBotoes();
    }

    /**
     * Processa mensagens de erro do servidor
     */
    private void processarErro(String mensagem) {
        String erro = mensagem.substring("ERRO:".length());
        rotuloEstado.setText("Erro: " + erro);
        JOptionPane.showMessageDialog(this, erro, "Erro", JOptionPane.ERROR_MESSAGE);
        
        if (erro.contains("Servidor lotado") || erro.contains("Jogo já em andamento") || 
            erro.contains("Servidor está a parar")) {
            desligar();
        }
    }

    /**
     * Alterna a marcação de um número no cartão
     * Só permite marcar números que já foram sorteados
     */
    private void alternarMarcacaoNumero(int indice) {
        if (!jogoIniciado) {
            rotuloEstado.setText("Aguarde o jogo começar para marcar números!");
            return;
        }

        int numero = cartao[indice];
        
        if (numerosMarcados.contains(numero)) {
            // Desmarcar número
            numerosMarcados.remove(numero);
            botoesCartao[indice].setBackground(numerosSorteados.contains(numero) ? Color.YELLOW : Color.WHITE);
            botoesCartao[indice].setBorderPainted(true);
            botoesCartao[indice].repaint();
            enviarMensagem("DESMARCAR:" + numero);
        } else {
            // Marcar número apenas se foi sorteado
            if (numerosSorteados.contains(numero)) {
                numerosMarcados.add(numero);
                botoesCartao[indice].setBackground(Color.GREEN);
                botoesCartao[indice].setOpaque(true);
                botoesCartao[indice].setBorderPainted(false);
                botoesCartao[indice].repaint();
                enviarMensagem("MARCAR:" + numero);
            } else {
                rotuloEstado.setText("Só pode marcar números que já foram sorteados!");
                JOptionPane.showMessageDialog(this, 
                    "O número " + numero + " ainda não foi sorteado!", 
                    "Número não sorteado", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Destaca um número no cartão quando é sorteado
     * Amarelo para sorteado mas não marcado, verde para marcado
     */
    private void destacarSeNoCartao(int numero) {
        for (int i = 0; i < 25; i++) {
            if (cartao[i] == numero) {
                if (!numerosMarcados.contains(numero)) {
                    botoesCartao[i].setBackground(Color.YELLOW);
                    botoesCartao[i].setOpaque(true);
                    botoesCartao[i].setBorderPainted(true);
                }
                break;
            }
        }
    }

    /**
     * Adiciona um número sorteado à lista visual
     * O último número aparece em destaque (negrito)
     */
    private void adicionarNumeroSorteado(int numero) {
        // Remove destaque do número anterior
        if (!rotulosNumerosSorteados.isEmpty()) {
            JLabel ultimoRotulo = rotulosNumerosSorteados.get(rotulosNumerosSorteados.size() - 1);
            ultimoRotulo.setFont(new Font("Arial", Font.PLAIN, 14));
        }
        
        // Criar novo rótulo para o número actual
        JLabel rotuloNumero = new JLabel(String.valueOf(numero));
        rotuloNumero.setFont(new Font("Arial", Font.BOLD, 16));
        rotuloNumero.setHorizontalAlignment(SwingConstants.CENTER);
        rotuloNumero.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        
        rotulosNumerosSorteados.add(rotuloNumero);
        painelNumerosSorteados.add(rotuloNumero);
        
        SwingUtilities.invokeLater(() -> {
            painelNumerosSorteados.revalidate();
            painelNumerosSorteados.repaint();
            
            // Garantir que o scroll vai para o final
            JScrollBar vertical = painelDeslizante.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
            
            rotuloNumero.scrollRectToVisible(rotuloNumero.getBounds());
        });
    }

    /**
     * Desactiva todos os botões quando o jogo termina
     */
    private void desactivarBotoes() {
        botaoLinha.setEnabled(false);
        botaoBingo.setEnabled(false);
        for (JButton botao : botoesCartao) {
            botao.setEnabled(false);
        }
    }

    /**
     * Envia uma mensagem para o servidor
     */
    private void enviarMensagem(String mensagem) {
        if (saida != null && ligado) {
            saida.println(mensagem);
            System.out.println("Enviado para servidor: " + mensagem);
        }
    }

    /**
     * Desliga do servidor e fecha todas as ligações
     */
    private void desligar() {
        ligado = false;
        try {
            if (entrada != null) entrada.close();
            if (saida != null) saida.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao desligar: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        desligar();
        super.dispose();
    }

    /**
     * Método principal - inicia a aplicação cliente
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeelLookAndFeel");
                } catch (Exception ex) {
                    System.out.println("A usar aparência padrão");
                }
            }
            new BingoClient();
        });
    }
}