import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * ChatWindow
 * - 플레이어 채팅을 위한 독립적인 대화 상자
 * - 서버에서 전달되는 채팅 메시지를 누적 표시하고, 사용자가 메시지를 전송할 수 있다.
 */
public class ChatWindow extends JDialog {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final NetworkHandler network;

    public ChatWindow(JFrame owner, NetworkHandler network) {
        super(owner, "플레이어 채팅", false);
        this.network = network;
        setSize(320, 420);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(chatArea);

        JButton sendButton = new JButton("전송");
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    /**
     * 서버에서 수신한 채팅 메시지를 창에 추가한다.
     */
    public void appendMessage(String message) {
        long systemTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
        String realTime = format.format(systemTime);

        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + realTime + "] " + message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        network.sendChat(text);
        inputField.setText("");
    }
}

