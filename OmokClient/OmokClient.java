import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * OmokClient
 * - 게임 클라이언트 진입점(main)을 제공한다.
 * - 사용자가 입력한 서버 주소로 접속하여 게임 윈도우(OmokFrame)를 띄운다.
 */
public class OmokClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

class LoginFrame extends JFrame {
    private final JTextField hostField = new JTextField("localhost", 15);
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("로그인");
    private final JButton registerButton = new JButton("회원가입");

    public LoginFrame() {
        setTitle("오목 로그인");
        setSize(360, 220);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        form.add(new JLabel("서버 주소"));
        form.add(hostField);
        form.add(new JLabel("아이디"));
        form.add(usernameField);
        form.add(new JLabel("비밀번호"));
        form.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.add(registerButton);
        buttonPanel.add(loginButton);

        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        add(form, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        loginButton.addActionListener(e -> attempt(NetworkHandler.AuthMode.LOGIN));
        registerButton.addActionListener(e -> attempt(NetworkHandler.AuthMode.REGISTER));
    }

    private void attempt(NetworkHandler.AuthMode mode) {
        String host = hostField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (host.isEmpty()) host = "localhost";
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("아이디와 비밀번호를 입력하세요.");
            return;
        }
        if (username.contains(" ") || password.contains(" ")) {
            statusLabel.setText("공백 없는 아이디/비밀번호를 사용하세요.");
            return;
        }

        toggleButtons(false);
        statusLabel.setText("서버에 연결 중...");

        final String targetHost = host;
        final String targetUsername = username;
        final String targetPassword = password;
        new Thread(() -> {
            try {
                NetworkHandler network = new NetworkHandler(targetHost, targetUsername, targetPassword, mode);
                SwingUtilities.invokeLater(() -> {
                    OmokFrame frame = new OmokFrame(network);
                    frame.setVisible(true);
                    dispose();
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("실패: " + ex.getMessage());
                    toggleButtons(true);
                });
            }
        }).start();
    }

    private void toggleButtons(boolean enabled) {
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }
}

/**
 * OmokFrame
 * - 실제 게임 윈도우를 구성하는 내부 JFrame 클래스
 * - TimerPanel, NetworkHandler, BoardPanel을 생성하고 배치한다.
 */
class OmokFrame extends JFrame {
    public OmokFrame(NetworkHandler network) {
        setTitle("네트워크 오목 - " + network.getUsername());
        setSize(600, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        
        TimerPanel timerPanel = new TimerPanel();
        network.setTimerPanel(timerPanel);
        BoardPanel board = new BoardPanel(network);
        ChatWindow chatWindow = new ChatWindow(this, network);
        network.setChatWindow(chatWindow);

        JButton chatToggleButton = new JButton("채팅 열기");
        chatToggleButton.addActionListener(e -> {
            boolean visible = chatWindow.isVisible();
            chatWindow.setVisible(!visible);
            chatToggleButton.setText(visible ? "채팅 열기" : "채팅 닫기");
        });
        chatWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatToggleButton.setText("채팅 열기");
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(timerPanel, BorderLayout.CENTER);
        topPanel.add(chatToggleButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        setVisible(true);
    }
}
