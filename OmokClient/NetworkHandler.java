
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.*;

/**
 * NetworkHandler
 * - 클라이언트 측 네트워크 통신을 담당한다.
 * - 서버에 소켓으로 접속하여 플레이어 ID를 수신하고, 서버로부터의 메시지를 수신하여
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.*;

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
    public enum AuthMode { LOGIN, REGISTER }

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private int playerId;
    private String username;
    private BoardPanel board;
    private TimerPanel timerPanel;
    private ChatWindow chatWindow;
    private JDialog currentDialog;


    /**
     * 서버에 연결하고 플레이어 ID를 수신한다. 실패 시 사용자에게 메시지를 띄운다.
     * 내부적으로 수신 루프를 새 스레드에서 실행한다.
     *
     * @param host 서버 호스트명 또는 IP
     */
    public NetworkHandler(String host, String username, String password, AuthMode mode) throws IOException {
        try {
            socket = new Socket(host, 5000);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            authenticate(username, password, mode);
            new Thread(this::listen).start();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * 수신한 이동을 반영할 BoardPanel을 설정한다.
     */
    public void setBoard(BoardPanel board) {
        this.board = board;
    }

    public void setTimerPanel(TimerPanel timerPanel) {
        this.timerPanel = timerPanel;
    }

    /**
     * 채팅 창에 대한 참조를 설정한다.
     */
    public void setChatWindow(ChatWindow chatWindow) {
        this.chatWindow = chatWindow;
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
                    if (board != null) {
                        board.updateBoard(x, y, pid);
                    }
                } else if (msg.startsWith("WIN")) {
                    int winner = Integer.parseInt(msg.split(" ")[1]);
                    if (board != null) {
                        board.handleWin(winner);
                    }
                } else if (msg.equals("RESET")) {
                    // 서버로부터 게임 초기화 신호 수신
                    if (board != null) {
                        board.handleReset();
                    }
                } else if (msg.startsWith("TIME")) {
                    int remainingTime = Integer.parseInt(msg.split(" ")[1]);
                    if (timerPanel != null) {
                        timerPanel.updateTime(remainingTime);
                    }
                } else if (msg.startsWith("TURN")) {
                    int currentPlayer = Integer.parseInt(msg.split(" ")[1]);
                    if (timerPanel != null) {
                        timerPanel.setCurrentPlayer(currentPlayer);
                    }
                } else if (msg.startsWith("START")) {
                    int startPlayer = Integer.parseInt(msg.split(" ")[1]);
                    if (timerPanel != null) {
                        timerPanel.setCurrentPlayer(startPlayer);
                        timerPanel.updateTime(35);
                        closeInfoMessage();
                    }
                } else if (msg.startsWith("CHAT")) {
                    if (chatWindow == null) continue;
                    String[] parts = msg.split(" ", 3);
                    if (parts.length < 3) continue;
                    int sender = Integer.parseInt(parts[1]);
                    String text = parts[2];
                    chatWindow.appendMessage((sender == 1 ? "(흑)" : "(백)") + text);
                } else if (msg.startsWith("REMATCH_PROMPT")) {
                    String requester = msg.length() > 15 ? msg.substring(15).trim() : "상대";
                    showInfoMessage(requester + "님이 다시하기를 신청했습니다.\n다시하기 버튼을 눌러 수락하세요.");
                } else if (msg.startsWith("REMATCH_WAIT")) {
                    String opponent = msg.length() > 13 ? msg.substring(13).trim() : "상대";
                    showInfoMessage(opponent + "님의 응답을 기다리는 중입니다.");
                    //다이얼로그 띄워도 어짜피 상대를 기다리는 다이얼로그에 씹혀서 채팅으로 알리는 게 좋을 것 같았습니다.
                } else if (msg.startsWith("REMATCH_ACCEPT")) {
                    String accepter = msg.length() > 15 ? msg.substring(15).trim() : "상대";
                    chatWindow.appendMessage(accepter + "님이 다시하기 요청을 수락했습니다. 새 게임을 시작합니다.");
                } else if (msg.startsWith("REMATCH_CANCEL")) {
                    chatWindow.appendMessage(msg.length() > 15 ? "상대가 게임을 떠났습니다." : "다시하기 요청이 취소되었습니다.");
                    //다이얼로그 띄워도 어짜피 상대를 기다리는 다이얼로그에 씹혀서 채팅으로 알리는 게 좋을 것 같았습니다.
                } else if (msg.startsWith("REMATCH_FAIL")) {
                    String reason = msg.length() > 13 ? msg.substring(13).trim() : "상대를 기다리는 중입니다.";
                    showInfoMessage(reason);
                } else if (msg.startsWith("REMATCH_ALREADY")) {
                    String detail = msg.length() > 17 ? msg.substring(17).trim() : "상대 응답을 기다리는 중입니다.";
                    showInfoMessage(detail);
                } else if (msg.equals("WAITING")) {
                    showInfoMessage("상대를 기다리는 중입니다.");
                } else if (msg.startsWith("PLAYER_INFO")) {
                    String[] p = msg.split(" ");
                    String blackName = p[1];
                    int blackWin = Integer.parseInt(p[2]);
                    int blackLose = Integer.parseInt(p[3]);
                    double blackRate = Double.parseDouble(p[4]);
                    String whiteName = p[5];
                    int whiteWin = Integer.parseInt(p[6]);
                    int whiteLose = Integer.parseInt(p[7]);
                    double whiteRate = Double.parseDouble(p[8]);

                    if (board != null) {
                        board.updatePlayerInfo(blackName, blackWin, blackLose, blackRate, whiteName, whiteWin, whiteLose, whiteRate);
                    }
                }
            }
        } catch (IOException ignored) {}
        finally {
            close();
        }
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

    /**
     * 채팅 메시지를 서버에 전송한다.
     */
    public void sendChat(String message) {
        try {
            out.writeUTF("CHAT " + message);
        } catch (IOException ignored) {}
    }

    public int getPlayerId() { return playerId; }

    public String getUsername() { return username; }

    private void showInfoMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dispose();
            }

            JOptionPane optionPane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
            currentDialog = optionPane.createDialog(board, "알림");
            currentDialog.setVisible(true);
        });
    }

    //매칭되면 상대를 기다리고 있다는 메시지 창이 자동으로 닫힙니다.
    private void closeInfoMessage() {
        SwingUtilities.invokeLater(() -> {
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dispose();
            }
        });
    }

    private void authenticate(String username, String password, AuthMode mode) throws IOException {
        out.writeUTF("AUTH " + mode.name() + " " + username + " " + password);
        String response = in.readUTF();
        if (response.startsWith("AUTH_OK")) {
            String[] parts = response.split(" ", 3);
            this.playerId = Integer.parseInt(parts[1]);
            this.username = parts.length >= 3 ? parts[2] : username;
        } else if (response.startsWith("AUTH_FAIL")) {
            String reason = response.length() > 9 ? response.substring(9).trim() : "인증 실패";
            throw new IOException(reason);
        } else {
            throw new IOException("알 수 없는 응답: " + response);
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
