import java.io.*;
import java.net.*;

/**
 * ClientHandler
 * - 서버에서 각 클라이언트 연결을 처리하는 스레드 클래스
 * - 클라이언트로부터 들어오는 메시지를 읽어 서버의 핸들러(server.handleMove 등)를 호출하고,
 *   서버에서의 브로드캐스트 메시지를 클라이언트로 전송할 수 있다.
 */
public class ClientHandler extends Thread {
    private Socket socket;
    private int playerId;
    private OmokServer server;
    private DataInputStream in;
    private DataOutputStream out;

    /**
     * 새 클라이언트 연결을 초기화하고 플레이어 ID를 클라이언트에 전송한다.
     *
     * @param socket 클라이언트 소켓
     * @param id 서버가 할당한 플레이어 ID
     * @param server 서버 참조 (이동 처리 등 호출용)
     */
    public ClientHandler(Socket socket, int id, OmokServer server) {
        this.socket = socket;
        this.playerId = id;
        this.server = server;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(playerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 수신 루프: 클라이언트로부터 MOVE 메시지를 읽어 서버의 handleMove 호출.
     * RESET 메시지를 받으면 게임 초기화.
     * 연결 종료 시 루프를 빠져나온다.
     */
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();
                if (msg.startsWith("MOVE")) {
                    String[] parts = msg.split(" ");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    server.handleMove(x, y, playerId);
                } else if (msg.equals("RESET")) {
                    // 클라이언트의 "다시하기" 요청 처리
                    server.handleReset();
                }
            }
        } catch (IOException e) {
            System.out.println("플레이어 " + playerId + " 연결 종료");
        }
    }

    /**
     * 이 클라이언트로 UTF 문자열 메시지를 전송한다.
     *
     * @param msg 전송할 메시지
     */
    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException ignored) {}
    }
}
