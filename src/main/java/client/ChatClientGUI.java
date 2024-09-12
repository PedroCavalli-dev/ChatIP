// File: client/ChatClientGUI.java
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatClientGUI extends JFrame {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private String user;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private boolean isConnected;
    private Thread msgListener;
    private boolean closing = false;
    private JButton emojiButton;

    public  ChatClientGUI() {
        setTitle("Chat Cliente");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationByPlatform(true);

        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.setPreferredSize(new Dimension(350, 30));
        messageField.setMinimumSize(new Dimension(350, 30));
        bottomPanel.add(messageField);

        sendButton = new JButton("Enviar");
        sendButton.setEnabled(false);
        sendButton.addActionListener(event -> {
            try {
                this.sendMessage(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.setMinimumSize(new Dimension(80, 30));
        bottomPanel.add(sendButton);
        add(bottomPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(sendButton);
        sendButton.requestFocus();

        Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 14); //
        messageField.setFont(emojiFont);
        chatArea.setFont(emojiFont);


        emojiButton = new JButton("\uD83D\uDE0A");
        emojiButton.setEnabled(false);
        emojiButton.setPreferredSize(new Dimension(50, 30));
        emojiButton.setMinimumSize(new Dimension(50, 30));
        bottomPanel.add(emojiButton);

        String[] emojis = {
                "\uD83D\uDE04", // 😄
                "\uD83E\uDD23", // 🤣
                "\uD83E\uDEE0", // 🫠
                "\uD83D\uDE07", // 😇
                "\uD83E\uDD70", // 🥰
                "\uD83D\uDE0D", // 😍
                "\uD83D\uDE1C", // 😜
                "\uD83E\uDD2A", // 🤪
                "\uD83D\uDE1D", // 😝
                "\uD83E\uDD11", // 🤑
                "\uD83E\uDD17", // 🤗
                "\uD83E\uDD10", // 🤐
                "\uD83D\uDE10", // 😐
                "\uD83D\uDE0F", // 😏
                "\uD83D\uDE12", // 😒
                "\uD83D\uDE14"  // 😔
        };

        JPopupMenu menuEmojis = new JPopupMenu();
        for (String emoji : emojis) {
            JMenuItem emojiItem = new JMenuItem(emoji);
            emojiItem.setPreferredSize(new Dimension(50, 30));
            emojiItem.setFont(emojiFont);
            emojiItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    messageField.setText(messageField.getText() + emoji);
                }
            });
            menuEmojis.add(emojiItem);
        }

        emojiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                menuEmojis.show(emojiButton, 0, emojiButton.getHeight());
            }
        });


        JPanel topPanel = new JPanel();
        connectButton = new JButton("Conectar");
        disconnectButton = new JButton("Desconectar");
        disconnectButton.setEnabled(false);
        topPanel.add(connectButton);
        topPanel.add(disconnectButton);
        add(topPanel, BorderLayout.NORTH);

        connectButton.addActionListener(this::connectToServer);
        disconnectButton.addActionListener(this::disconnectFromServer);

        setVisible(true);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (JOptionPane.showConfirmDialog(ChatClientGUI.this,
                        "Você tem certeza que quer sair da conversa?", "Fechar a conversa?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    disconnectFromServer(null);
                    System.exit(0);
                }
            }
        });
    }

    private void connectToServer(ActionEvent event) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            chatArea.append("Conectado ao servidor.\n");

            if (user == null) {
                user = IOGUI.readStrGUI("Nome de Usuário", "Nome de usuário");
            }

            isConnected = true;
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            emojiButton.setEnabled(true);

            // Thread to listen for messages from the server
            msgListener = new Thread(() -> {
                Object input;

                try {
                    while ((input = in.readObject()) != null) {
                        if (input instanceof Message) {
                            Message msg = (Message) input;
                            chatArea.append(msg.getUsername() + ": " + msg.getMsg() + "\n");
                        } else {
                            chatArea.append("Erro ao decodificar o tipo do dado recebido!");
                        }
                    }
                } catch (IOException e) {
                    if (!closing) {
                        chatArea.append("Erro de conexão: " + e.getMessage() + "\n");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            msgListener.start();

        } catch (IOException e) {
            chatArea.append("Não foi possível conectar ao servidor: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage(ActionEvent event) throws IOException {
        if (isConnected && !messageField.getText().trim().isEmpty()) {
            Message msg = new Message(user, messageField.getText().trim());
            out.writeObject(msg);
            chatArea.append("Você: " + messageField.getText().trim() + "\n");
            messageField.setText("");
        }
    }

    private void disconnectFromServer(ActionEvent event) {
        try {
            if (socket != null && !socket.isClosed()) {
                closing = true;

                socket.close();
            }
            isConnected = false;
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            chatArea.append("Desconectado do servidor.\n");
        } catch (IOException e) {
            chatArea.append("Erro ao desconectar: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
