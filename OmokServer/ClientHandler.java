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
    private boolean authenticated = false;
    private String username = "";

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
                if (!authenticated) {
                    handleAuth(msg);
                    continue;
                }
                if (msg.startsWith("MOVE")) {
                    String[] parts = msg.split(" ");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    server.handleMove(x, y, playerId);
                } else if (msg.equals("RESET")) {
                    // 클라이언트의 "다시하기" 요청 처리
                    server.handleReset(playerId);
                } else if (msg.startsWith("CHAT")) {
                    String text = msg.length() > 5 ? msg.substring(5) : "";
                    server.handleChat(playerId, text);
                }
            }
        } catch (IOException e) {
            System.out.println("플레이어 " + playerId + " 연결 종료");
        } finally {
            closeResources();
            server.removeClient(this);
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

    public int getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    private void handleAuth(String msg) throws IOException {
        if (!msg.startsWith("AUTH")) {
            out.writeUTF("AUTH_FAIL 인증이 필요합니다.");
            return;
        }
        String[] parts = msg.split(" ", 4);
        if (parts.length < 4) {
            out.writeUTF("AUTH_FAIL 형식이 올바르지 않습니다.");
            return;
        }
        String mode = parts[1];
        String user = parts[2];
        String pass = parts[3];

        if (user.trim().isEmpty() || pass.trim().isEmpty()) {
            out.writeUTF("AUTH_FAIL 아이디/비밀번호를 입력하세요.");
            return;
        }

        UserManager userManager = server.getUserManager();
        boolean proceed = false;

        if ("REGISTER".equalsIgnoreCase(mode)) {
            if (userManager.register(user, pass)) {
                proceed = true;
            } else {
                out.writeUTF("AUTH_FAIL 이미 존재하는 아이디입니다.");
                return;
            }
        }

        if ("LOGIN".equalsIgnoreCase(mode) || proceed) {
            if (!userManager.authenticate(user, pass)) {
                out.writeUTF("AUTH_FAIL 아이디 또는 비밀번호가 올바르지 않습니다.");
                return;
            }
        } else {
            out.writeUTF("AUTH_FAIL 지원하지 않는 명령입니다.");
            return;
        }

        this.username = user;
        this.authenticated = true;
        server.registerPlayerName(playerId, username);
        out.writeUTF("AUTH_OK " + playerId + " " + username);
        for (String chatLine : server.getChatHistory()) {
            out.writeUTF(chatLine);
        }
        server.registerClient(this);
    }

    private void closeResources() {
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
