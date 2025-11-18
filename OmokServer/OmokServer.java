
import java.io.*;
import java.net.*;
import java.util.*;

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
    private static final int TIME_LIMIT = 35; // 35초 시간 제한
    private int remainingTime = TIME_LIMIT;
    private boolean gameActive = false;
    private Object timerLock = new Object();

    /**
     * 서버를 시작하고 포트 5000에서 클라이언트 연결을 기다린다.
     * 두 명이 연결되면 게임을 시작(START 메시지 브로드캐스트).
     */
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(5000);
        System.out.println("[SERVER] 오목 서버가 시작되었습니다. (port: 5000)");

        while (true) {
            Socket socket = serverSocket.accept();
            ClientHandler client = new ClientHandler(socket, clients.size() + 1, this);
            clients.add(client);
            client.start();
            System.out.println("플레이어 " + clients.size() + " 연결됨");
            if (clients.size() == 2) {
                broadcast("START " + gameBoard.getCurrentTurn());
                gameActive = true;
                remainingTime = TIME_LIMIT;
                startTimer();
                System.out.println("두 명이 모두 연결되었습니다. 게임 시작!");
            }
        }
    }

    /**
     * 각 턴마다 시간 제한을 관리하는 타이머를 시작한다.
     */
    private void startTimer() {
        new Thread(() -> {
            while (gameActive) {
                try {
                    Thread.sleep(1000); // 1초마다 업데이트
                    synchronized (timerLock) {
                        if (gameActive && remainingTime > 0) {
                            remainingTime--;
                            broadcast("TIME " + remainingTime);
                            
                            if (remainingTime == 0) {
                                // 시간 종료, 턴 넘김
                                handleTimeOut();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
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
     * - 유효성 검사 후 보드에 반영하고 모든 클라이언트에 MOVE 메시지를 전송
     * - 승리 시 WIN 메시지를 브로드캐스트하고 결과를 저장
     * - 턴 변경 시 시간 초기화
     */
    public synchronized void handleMove(int x, int y, int playerId) {
        if (!gameBoard.isValidMove(x, y, playerId)) return;

        gameBoard.placeStone(x, y, playerId);
        broadcast("MOVE " + x + " " + y + " " + playerId);

        if (gameBoard.checkWin(x, y, playerId)) {
            broadcast("WIN " + playerId);
            gameBoard.saveResult("Player" + playerId + " 승리");
            gameActive = false; // 게임 종료
        } else {
            gameBoard.switchTurn();
            remainingTime = TIME_LIMIT;
            broadcast("TURN " + gameBoard.getCurrentTurn());
            broadcast("TIME " + remainingTime);
        }
    }

    /**
     * 클라이언트의 "다시하기" 요청을 처리하여 게임을 초기화한다.
     */
    public synchronized void handleReset() {
        gameBoard.resetGame();
        gameActive = true;
        remainingTime = TIME_LIMIT;
        broadcast("RESET");
        broadcast("START " + gameBoard.getCurrentTurn());
        System.out.println("게임이 초기화되었습니다.");
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
