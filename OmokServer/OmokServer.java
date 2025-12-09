import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime;

/**
 * OmokServer
 * - 오목 게임의 서버 진입점이자 게임 상태를 관리하는 클래스
 * - 클라이언트 연결을 수락하고 ClientHandler를 생성하여 클라이언트별 스레드를 실행한다.
 * - 클라이언트로부터 온 이동을 검증하고 브로드캐스트하며, 승리 시 결과를 기록한다.
 * - 각 턴마다 35초의 시간 제한을 관리한다.
 */
public class OmokServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private GameBoard gameBoard = new GameBoard();
    private List<String> chatHistory = new ArrayList<>();
    private Map<Integer, String> playerNames = new HashMap<>();
    private final UserManager userManager = new UserManager("users.db");
    private Queue<Integer> availableSlots = new ArrayDeque<>();
    private int rematchRequester = -1;
    private static final int TIME_LIMIT = 35; // 35초 시간 제한
    private int remainingTime = TIME_LIMIT;
    private boolean gameActive = false;
    private Object timerLock = new Object();
    private Thread timerThread;

    public OmokServer() {
        availableSlots.offer(1);
        availableSlots.offer(2);
    }

    /**
     * 서버를 시작하고 포트 5000에서 클라이언트 연결을 기다린다.
     * 두 명이 연결되면 게임을 시작(START 메시지 브로드캐스트).
     */
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(5000);
        System.out.println("[SERVER] 오목 서버가 시작되었습니다. (port: 5000)");

        while (true) {
            Socket socket = serverSocket.accept();
            int slot = acquireSlot();
            if (slot == -1) {
                System.out.println("새 연결 거부: 슬롯 부족");
                try (DataOutputStream tempOut = new DataOutputStream(socket.getOutputStream())) {
                    tempOut.writeUTF("SERVER_FULL");
                } catch (IOException ignored) {}
                socket.close();
                continue;
            }
            ClientHandler client = new ClientHandler(socket, slot, this);
            client.start();
            System.out.println("새 클라이언트 연결 (슬롯 " + slot + ")");
        }
    }

    /**
     * 각 턴마다 시간 제한을 관리하는 타이머를 시작한다.
     */
    private void startTimer() {
        stopTimerThread();
        timerThread = new Thread(() -> {
            while (gameActive) {
                try {
                    Thread.sleep(1000);
                    sendPlayerInfoToClients();
                    //플레이어 정보를 계속 갱신합니다. 가끔 한 쪽에서 플레이어 정보를 못 읽고 누락시키는 버그가 있는데
                    //정확한 원인을 모르겠어서 누락시켜도 초마다 계속 갱신시키도록 만들어봤습니다...
                    synchronized (timerLock) {
                        if (gameActive && remainingTime > 0) {
                            remainingTime--;
                            broadcast("TIME " + remainingTime);

                            if (remainingTime == 0) {
                                handleTimeOut();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timerThread.start();
    }

    private void stopTimerThread() {
        if (timerThread != null) {
            timerThread.interrupt();
            timerThread = null;
        }
    }

    /**
     * 시간 제한이 끝났을 때 처리 (턴을 자동으로 넘김)
     */
    private synchronized void handleTimeOut() {
        System.out.println("플레이어 " + gameBoard.getCurrentTurn() + "의 시간이 종료되었습니다.");
        gameBoard.switchTurn();
        remainingTime = TIME_LIMIT;
        broadcast("TURN " + gameBoard.getCurrentTurn());
        broadcast("TIME " + remainingTime);
    }

    /**
     * 클라이언트로부터 온 이동 요청을 처리한다.
     */
    public synchronized void handleMove(int x, int y, int playerId) {
        if (!gameBoard.isValidMove(x, y, playerId)) return;

        gameBoard.placeStone(x, y, playerId);
        broadcast("MOVE " + x + " " + y + " " + playerId);

        if (gameBoard.checkWin(x, y, playerId)) {
            broadcast("WIN " + getPlayerName(playerId));
            recordWin(playerId);   // ← 여기서 ID 기반 저장

            gameActive = false;
        } else {
            gameBoard.switchTurn();
            remainingTime = TIME_LIMIT;
            broadcast("TURN " + gameBoard.getCurrentTurn());
            broadcast("TIME " + remainingTime);
        }
    }

    /**
     * 경기 기록 저장
     */
    public synchronized void recordWin(int winnerId) {
        int loserId = getOpponentId(winnerId);

        String winnerName = playerNames.getOrDefault(winnerId, "Player" + winnerId);
        String loserName  = playerNames.getOrDefault(loserId, "Player" + loserId);

        String line = LocalDateTime.now() + 
            " - " + winnerName + " 승리 / " + loserName + " 패배\n";

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("record.txt", true))) {
            bw.write(line);
        } catch (IOException ignored) {}

        System.out.println("기록 저장됨: " + line);
    }

    public int getWins(String username) {
        int wins = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("record.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
            if (line.contains(username + " 승리")) wins++;
            }
        } catch (IOException ignored) {}
        return wins;
    }

    public int getLosses(String username) {
        int losses = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("record.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(username + " 패배")) losses++;
            }
        } catch (IOException ignored) {}
        return losses;
    }

    /**
     * "다시하기" 요청 처리
     */
    public synchronized void handleReset(int playerId) {
        if (clients.size() < 2) {
            sendToPlayer(playerId, "REMATCH_FAIL 상대를 기다리는 중입니다.");
            return;
        }

        if (rematchRequester == -1) {
            rematchRequester = playerId;
            int opponentId = getOpponentId(playerId);
            String requesterName = getPlayerName(playerId);
            String opponentName = opponentId == -1 ? "상대" : getPlayerName(opponentId);
            sendToPlayer(playerId, "REMATCH_WAIT " + opponentName);
            if (opponentId != -1) {
                sendToPlayer(opponentId, "REMATCH_PROMPT " + requesterName);
            }
            System.out.println("사용자 [" + requesterName + "] 가 다시하기를 요청했습니다.");
            return;
        }

        if (rematchRequester == playerId) {
            sendToPlayer(playerId, "REMATCH_ALREADY 상대 응답을 기다리는 중입니다.");
            return;
        }

        String accepterName = getPlayerName(playerId);
        broadcast("REMATCH_ACCEPT " + accepterName);
        startNewMatch();
        System.out.println("사용자 [" + accepterName + "] 가 다시하기 요청을 수락했습니다.");
    }

    public synchronized void handleChat(int playerId, String message) {
        if (message == null) return;
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return;

        String displayName = playerNames.getOrDefault(playerId, "Player" + playerId);
        String formatted = "CHAT " + playerId + " " + displayName + " : " + trimmed;
        chatHistory.add(formatted);
        if (chatHistory.size() > 100) {
            chatHistory.remove(0);
        }
        broadcast(formatted);
    }

    public synchronized List<String> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public synchronized void registerPlayerName(int playerId, String username) {
        playerNames.put(playerId, username);
        System.out.println("사용자 [" + username + "] 가 슬롯 " + playerId + "로 로그인했습니다.");
    }

    public synchronized String getPlayerName(int playerId) {
        return playerNames.getOrDefault(playerId, "Player" + playerId);
    }

    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
        String name = playerNames.remove(handler.getPlayerId());
        String display = name != null ? name : "Player" + handler.getPlayerId();
        System.out.println("사용자 [" + display + "] 연결 종료 (슬롯 " + handler.getPlayerId() + ")");
        releaseSlot(handler.getPlayerId());

        if (rematchRequester != -1) {
            int notifyTarget = rematchRequester == handler.getPlayerId()
                    ? getOpponentId(handler.getPlayerId())
                    : rematchRequester;
            if (notifyTarget != -1 && notifyTarget != handler.getPlayerId()) {
                sendToPlayer(notifyTarget, "REMATCH_CANCEL 상대가 게임을 떠났습니다.");

            }
            rematchRequester = -1;
        }

        if (clients.size() < 2) {
            stopTimerThread();
            gameActive = false;
            gameBoard.resetGame();
            remainingTime = TIME_LIMIT;
            broadcast("WAITING");
            System.out.println("접속자가 2명 미만으로 떨어져 게임을 대기 상태로 초기화했습니다.");
        }
    }

    public synchronized void registerClient(ClientHandler handler) {
        if (!clients.contains(handler)) {
            clients.add(handler);
            if (clients.size() == 2) {
                startNewMatch();
            } else {
                broadcast("WAITING");
                System.out.println("한 명이 접속했습니다. 상대를 기다리는 중입니다.");
            }
        }
    }

    private synchronized void startNewMatch() {
        if (clients.size() < 2) return;
        stopTimerThread();
        gameActive = false;
        rematchRequester = -1;
        gameBoard.resetGame();
        remainingTime = TIME_LIMIT;
        gameActive = true;

        int startPlayer = gameBoard.getCurrentTurn();
//        if (startPlayer == -1) {
//            startPlayer = 1;
//            gameBoard.setCurrentTurn(startPlayer);
//        }

        broadcast("RESET");
        broadcast("START " + startPlayer);
        startTimer();
        System.out.println("두 명이 모두 연결되었습니다. 게임 시작!");
    }

//    public synchronized void sendPlayerInfoToClients() {
//        if (clients.size() < 2) return;
//
//        int p1 = clients.get(0).getPlayerId();
//        int p2 = clients.get(1).getPlayerId();
//
//        String name1 = getPlayerName(p1);
//        String name2 = getPlayerName(p2);
//
//        int wins1 = getWins(name1);
//        int losses1 = getLosses(name1);
//        int wins2 = getWins(name2);
//        int losses2 = getLosses(name2);
//
//        double rate1 = wins1 + losses1 > 0 ? (wins1 * 100.0 / (wins1 + losses1)) : 0;
//        double rate2 = wins2 + losses2 > 0 ? (wins2 * 100.0 / (wins2 + losses2)) : 0;
//
//        String msg = String.format(
//            "PLAYER_INFO %s %.2f %s %.2f",
//            name1, rate1,
//            name2, rate2
//        );
//
//        broadcast(msg);
//    } 기존의 방식은 clients에서 플레이어 아이디를 뽑아오는 방식이라 꼬이기 쉬움

    private void sendPlayerInfoToClients() {
        String blackName = getPlayerName(1); // playerId 1 = 흑
        String whiteName = getPlayerName(2); // playerId 2 = 백

        int wins1 = getWins(blackName), losses1 = getLosses(blackName);
        int wins2 = getWins(whiteName), losses2 = getLosses(whiteName);

        double rate1 = wins1 + losses1 > 0 ? (wins1 * 100.0 / (wins1 + losses1)) : 0;
        double rate2 = wins2 + losses2 > 0 ? (wins2 * 100.0 / (wins2 + losses2)) : 0;

        String msg = String.format("PLAYER_INFO %s %d %d %.2f %s %d %d %.2f", blackName, wins1, losses1, rate1, whiteName, wins2, losses2, rate2);
        broadcast(msg);
    }


    private void sendToPlayer(int playerId, String msg) {
        for (ClientHandler c : clients) {
            if (c.getPlayerId() == playerId) {
                c.sendMessage(msg);
                break;
            }
        }
    }

    private int getOpponentId(int playerId) {
        for (ClientHandler c : clients) {
            if (c.getPlayerId() != playerId) {
                return c.getPlayerId();
            }
        }
        return -1;
    }

    private synchronized int acquireSlot() {
        Integer slot = availableSlots.poll();
        return slot == null ? -1 : slot;
    }

    private synchronized void releaseSlot(int slot) {
        if (!availableSlots.contains(slot)) {
            availableSlots.offer(slot);
        }
    }

    /**
     * 연결된 모든 클라이언트에 메시지를 전송한다.
     */
    public synchronized void broadcast(String msg) {
        for (ClientHandler c : clients) c.sendMessage(msg);
    }

    /**
     * 서버 진입점
     */
    public static void main(String[] args) throws IOException {
        new OmokServer().startServer();
    }
}
