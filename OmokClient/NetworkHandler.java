
import javax.swing.*;
import java.io.*;
import java.net.*;

/**
 * NetworkHandler
 * - 클라이언트 측 네트워크 통신을 담당한다.
 * - 서버에 소켓으로 접속하여 플레이어 ID를 수신하고, 서버로부터의 메시지를 수신하여
 *   BoardPanel에 반영한다.
 *
 * 주요 책임:
 * - 서버 접속 및 입출력 스트림 관리
 * - 별도 스레드에서 listen()을 실행하여 서버 메시지(MOVE, WIN 등)를 처리
 * - 사용자의 이동을 서버에 전송(sendMove)
 * - 시간 정보 수신 및 표시 (TIME, TURN 메시지)
 */
public class NetworkHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int playerId;
    private BoardPanel board;
    private TimerPanel timerPanel;

    /**
     * 서버에 연결하고 플레이어 ID를 수신한다. 실패 시 사용자에게 메시지를 띄운다.
     * 내부적으로 수신 루프를 새 스레드에서 실행한다.
     *
     * @param host 서버 호스트명 또는 IP
     */
    public NetworkHandler(String host, TimerPanel timerPanel) {
        this.timerPanel = timerPanel;
        try {
            socket = new Socket(host, 5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            playerId = in.readInt();
            JOptionPane.showMessageDialog(null, "플레이어 " + playerId + "으로 접속했습니다.");
            new Thread(() -> listen()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "서버 연결 실패: " + e.getMessage());
        }
    }

    /**
     * 수신한 이동을 반영할 BoardPanel을 설정한다.
     */
    public void setBoard(BoardPanel board) {
        this.board = board;
    }

    /**
     * 서버로부터 들어오는 UTF 메시지를 읽어 처리한다.
     * - MOVE x y pid : 보드 갱신
     * - WIN pid : 승리 알림
     * - RESET : 게임 보드 초기화
     * - TIME seconds : 남은 시간 업데이트
     * - TURN pid : 턴 변경 알림
     * - START : 게임 시작
     */
    private void listen() {
        try {
            while (true) {
                String msg = in.readUTF();
                if (msg.startsWith("MOVE")) {
                    String[] p = msg.split(" ");
                    int x = Integer.parseInt(p[1]);
                    int y = Integer.parseInt(p[2]);
                    int pid = Integer.parseInt(p[3]);
                    board.updateBoard(x, y, pid);
                } else if (msg.startsWith("WIN")) {
                    int winner = Integer.parseInt(msg.split(" ")[1]);
                    board.handleWin(winner);
                } else if (msg.equals("RESET")) {
                    // 서버로부터 게임 초기화 신호 수신
                    board.handleReset();
                } else if (msg.startsWith("TIME")) {
                    int remainingTime = Integer.parseInt(msg.split(" ")[1]);
                    timerPanel.updateTime(remainingTime);
                } else if (msg.startsWith("TURN")) {
                    int currentPlayer = Integer.parseInt(msg.split(" ")[1]);
                    timerPanel.setCurrentPlayer(currentPlayer);
                } else if (msg.startsWith("START")) {
                    int startPlayer = Integer.parseInt(msg.split(" ")[1]);
                    timerPanel.setCurrentPlayer(startPlayer);
                    timerPanel.updateTime(35);
                }
            }
        } catch (IOException ignored) {}
    }

    public void sendMove(int x, int y) {
        try {
            out.writeUTF("MOVE " + x + " " + y);
        } catch (IOException ignored) {}
    }

    /**
     * 게임 다시하기 신호를 서버에 전송한다.
     */
    public void sendReset() {
        try {
            out.writeUTF("RESET");
        } catch (IOException ignored) {}
    }

    public int getPlayerId() { return playerId; }
}
