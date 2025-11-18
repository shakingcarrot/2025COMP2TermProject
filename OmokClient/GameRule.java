
/**
 * GameRule
 * - 오목의 승리 조건, 금수(금지된 수), 무승부 판정 관련 유틸리티 클래스로 정적 메서드만 제공한다.
 * - 보드 사이즈는 내부 상수 SIZE로 정의되어 있으며, 정확히 5연속일 때만 승리를 판정한다.
 *
 * 적용 규칙:
 * - 정확히 5개 연속 (장목 금지): 6개 이상은 승리 아님
 * - 3-3 금수: 흑이 3-3을 만드는 수는 금지 (백은 금수 없음, 흑만 적용)
 * - 제공하는 기능:
 *   - checkWin(): 정확히 5개 연속 판정
 *   - isForbidden(): 해당 수가 금수인지 확인 (3-3 포함)
 *   - isDraw(): 무승부 확인
 */
public class GameRule {
    private static final int SIZE = 15;

    /**
     * 현재 보드 상태에서 해당 위치(x, y)에 player의 돌을 두었을 때
     * 정확히 5개 연속인지 확인한다 (6개 이상은 승리 아님).
     *
     * @param board 현재 보드(15x15, 0=빈칸,1=흑,2=백)
     * @param x 돌을 둔 x 좌표(0-14)
     * @param y 돌을 둔 y 좌표(0-14)
     * @param player 플레이어 ID (1 또는 2)
     * @return 정확히 5개 연속이면 true, 4개 이하 또는 6개 이상이면 false
     */
    public static boolean checkWin(int[][] board, int x, int y, int player) {
        // 가로, 세로, 대각선(↘, ↗) 방향 검사
        int[][] directions = {
                {1, 0},  // 가로
                {0, 1},  // 세로
                {1, 1},  // ↘ 대각선
                {1, -1}  // ↗ 대각선
        };

        for (int[] dir : directions) {
            int count = 1; // 현재 위치 포함
            count += countStones(board, x, y, dir[0], dir[1], player);
            count += countStones(board, x, y, -dir[0], -dir[1], player);
            
            // 정확히 5개일 때만 승리 (6개 이상인 장목은 승리 아님)
            if (count == 5)
                return true;
        }
        return false;
    }

    /**
     * 주어진 방향(dx, dy)으로 연속된 같은 색 돌 개수를 센다.
     *
     * @return 해당 방향으로 연속된 돌의 개수(현재 위치 제외)
     */
    private static int countStones(int[][] board, int x, int y, int dx, int dy, int player) {
        int count = 0;
        int nx = x + dx;
        int ny = y + dy;

        while (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && board[nx][ny] == player) {
            count++;
            nx += dx;
            ny += dy;
        }
        return count;
    }

    /**
     * 해당 위치(x, y)에 돌을 두는 것이 금수인지 확인한다.
     * - 3-3 금수: 흑(player=1)이 양쪽에서 3-3을 만드는 경우만 금지
     * - 백(player=2)은 금수 없음 (흑만 금수 적용)
     *
     * @param board 현재 보드
     * @param x x 좌표
     * @param y y 좌표
     * @param player 플레이어 ID (1=흑, 2=백)
     * @return 금수이면 true
     */
    public static boolean isForbidden(int[][] board, int x, int y, int player) {
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
                // 왼쪽 끝이 비어있는지, 오른쪽 끝이 비어있는지 확인
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
    private static boolean isOpenEnd(int[][] board, int x, int y, int dx, int dy, int player) {
        // 현재 방향으로 연속된 돌을 따라가서 끝 다음 칸 확인
        int nx = x + dx;
        int ny = y + dy;
        
        while (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && board[nx][ny] == player) {
            nx += dx;
            ny += dy;
        }
        
        // 끝 다음 칸이 범위 내이면서 비어있으면 열려있음
        return nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && board[nx][ny] == 0;
    }

    /**
     * 게임판이 가득 차서 무승부인지 확인한다.
     *
     * @param board 현재 보드
     * @return 빈 칸이 하나라도 있으면 false, 아니면 true
     */
    public static boolean isDraw(int[][] board) {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                if (board[i][j] == 0)
                    return false;
        return true;
    }
}
