import javax.swing.*;
import java.awt.*;

/**
 * OmokClient
 * - 게임 클라이언트 진입점(main)을 제공한다.
 * - 사용자가 입력한 서버 주소로 접속하여 게임 윈도우(OmokFrame)를 띄운다.
 */
public class OmokClient {
    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog("서버 IP 입력 (기본: localhost)");
        if (host == null || host.isEmpty()) host = "localhost";
        new OmokFrame(host);
    }
}

/**
 * OmokFrame
 * - 실제 게임 윈도우를 구성하는 내부 JFrame 클래스
 * - TimerPanel, NetworkHandler, BoardPanel을 생성하고 배치한다.
 */
class OmokFrame extends JFrame {
    public OmokFrame(String host) {
        setTitle("네트워크 오목");
        setSize(600, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        
        TimerPanel timerPanel = new TimerPanel();
        NetworkHandler network = new NetworkHandler(host, timerPanel);
        BoardPanel board = new BoardPanel(network);
        
        add(timerPanel, BorderLayout.NORTH);
        add(board, BorderLayout.CENTER);
        setVisible(true);
    }
}
