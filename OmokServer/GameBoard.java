import java.io.*;
import java.time.LocalDateTime;

/**
 * GameBoard
 * - 서버 측에서 게임 상태(보드, 현재 턴)를 관리하는 클래스
 * - 유효성 검사(금수 포함), 돌 배치, 승리 판정(장목 제외), 결과 저장 기능을 제공한다.
 *
 * 규칙:
 * - 정확히 5개 연속: 승리 (6개 이상 장목은 승리 아님)
 * - 3-3 금수: 흑만 적용 (양쪽에서 3이 되는 수 금지)
 */
public class GameBoard {
    private int[][] board = new int[15][15];
    private int currentTurn = 1;
    private static final int SIZE = 15;

    /**
     * 해당 플레이어가 (x,y)에 돌을 둘 수 있는지 검사한다.
     * - 범위, 빈칸, 현재 턴 확인
     * - 3-3 금수 검사 (흑만 적용)
     *
     * @return 유효한 이동이면 true
     */
    public boolean isValidMove(int x, int y, int playerId) {
        // 기본 유효성 검사
        if (!(x >= 0 && y >= 0 && x < 15 && y < 15 && board[x][y] == 0 && playerId == currentTurn))
            return false;
        
        // 3-3 금수 검사 (흑만)
        if (playerId == 1 && isForbidden(board, x, y, playerId)) {
            return false;
        }
        
        return true;
    }

    /**
     * 해당 위치(x, y)에 돌을 두는 것이 금수인지 확인한다.
     * - 3-3 금수: 흑이 양쪽에서 3-3을 만드는 경우만 금지
     */
    private boolean isForbidden(int[][] board, int x, int y, int player) {
        // 흑(player=1)만 3-3 금수 적용
        if (player != 1) return false;
        
        // 임시로 돌을 놓음
        board[x][y] = player;
        
        // 3-3 금지 검사: 정확히 3이 양쪽에서 나오는지 확인
        int threeCount = 0;
        
        int[][] directions = {
                {1, 0},  // 가로
                {0, 1},  // 세로
                {1, 1},  // ↘ 대각선
                {1, -1}  // ↗ 대각선
        };
        
        for (int[] dir : directions) {
            int count = 1; // 현재 위치
            int left = countStones(board, x, y, dir[0], dir[1], player);
            int right = countStones(board, x, y, -dir[0], -dir[1], player);
            count += left + right;
            
            // 양쪽이 모두 비어있고 (또는 경계), 정확히 3이 되는 경우
            if (count == 3) {
                boolean leftOpen = isOpenEnd(board, x, y, -dir[0], -dir[1], player);
                boolean rightOpen = isOpenEnd(board, x, y, dir[0], dir[1], player);
                
                // 양쪽이 모두 열려있으면 3-3 가능성 증가
                if (leftOpen && rightOpen) {
                    threeCount++;
                }
            }
        }
        
        // 임시 놓은 돌 제거
        board[x][y] = 0;
        
        // 3이 2개 이상이면 3-3 금수
        return threeCount >= 2;
    }

    /**
     * 특정 방향의 끝이 "열려있는지" 확인 (그 다음이 빈 칸 또는 경계)
     */
    private boolean isOpenEnd(int[][] board, int x, int y, int dx, int dy, int player) {
        // 현재 방향으로 연속된 돌을 따라가서 끝 다음 칸 확인
        int nx = x + dx;
        int ny = y + dy;
        
        while (nx >= 0 && ny >= 0 && nx < 15 && ny < 15 && board[nx][ny] == player) {
            nx += dx;
            ny += dy;
        }
        
        // 끝 다음 칸이 범위 내이면서 비어있으면 열려있음
        return nx >= 0 && ny >= 0 && nx < 15 && ny < 15 && board[nx][ny] == 0;
    }

    /**
     * 보드에 돌을 놓는다. 호출 전 isValidMove로 검사되어야 한다.
     */
    public void placeStone(int x, int y, int playerId) {
        board[x][y] = playerId;
    }

    /**
     * 주어진 방향(dx, dy)으로 연속된 같은 색 돌 개수를 센다.
     */
    private int countStones(int[][] board, int x, int y, int dx, int dy, int player) {
        int count = 0;
        int nx = x + dx;
        int ny = y + dy;

        while (nx >= 0 && ny >= 0 && nx < 15 && ny < 15 && board[nx][ny] == player) {
            count++;
            nx += dx;
            ny += dy;
        }
        return count;
    }

    /**
     * 현재 턴을 상대 플레이어로 변경한다.
     */
    public void switchTurn() {
        currentTurn = (currentTurn == 1) ? 2 : 1;
    }

    /**
     * 현재 턴 플레이어를 반환한다.
     */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /**
     * 현재 보드에서 (x,y)에 놓인 돌로 인해 정확히 5개 연속(승리)인지 판정한다.
     * 6개 이상 장목은 승리가 아님.
     *
     * @return 정확히 5개 연속이면 true
     */
    public boolean checkWin(int x, int y, int playerId) {
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int count = 1;
            count += countStones(board, x, y, d[0], d[1], playerId);
            count += countStones(board, x, y, -d[0], -d[1], playerId);
            if (count == 5) return true;  // 정확히 5개만 승리
        }
        return false;
    }

    /**
     * 특정 방향으로 연속된 돌 개수를 센다 (현재 위치 제외).
     */
    /**
     * 게임 결과를 기록 파일(record.txt)에 저장한다.
     */
    public void saveResult(String result) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("record.txt", true))) {
            bw.write(LocalDateTime.now() + " - " + result + "\n");
        } catch (IOException ignored) {}
    }

    /**
     * 게임을 초기화하여 새 게임을 시작할 준비를 한다.
     */
    public void resetGame() {
        board = new int[15][15];
        currentTurn = 1;
    }
    
}
