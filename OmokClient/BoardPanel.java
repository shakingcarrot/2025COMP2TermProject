import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * BoardPanel
 * - GUI에서 오목판을 그리며 사용자의 마우스 입력을 받아 서버로 이동을 전송한다.
 * - 내부적으로 15x15 정수 배열(board)을 유지하여 돌(흑=1, 백=2)을 그린다.
 * - 서버로부터 도착한 이동 정보를 받아 보드를 갱신(updateBoard)하고 승리/무승부를 알린다.
 * - 마우스 호버 시 반투명한 돌로 위치를 미리 표시한다.
 *
 * 주요 책임:
 * - 보드 렌더링(paintComponent)
 * - 사용자 클릭 처리(mouseClicked) → NetworkHandler.sendMove 호출
 * - 서버에서 온 이동을 반영(updateBoard)
 * - 마우스 이동 감지 및 호버 위치 미리보기(mouseMotionListener)
 */
public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {
    private int[][] board = new int[15][15];
    private NetworkHandler network;
    private int playerId;
    private int hoverX = -1;
    private int hoverY = -1;
    // ---------------------------------------------
    // ⭐ 추가된 필드: 흑/백 플레이어 이름 + 승률
    // ---------------------------------------------
    private String blackInfo = "흑";
    private String whiteInfo = "백";
    // ---------------------------------------------

    public BoardPanel(NetworkHandler network) {
        this.network = network;
        this.playerId = network.getPlayerId();
        network.setBoard(this);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    // ---------------------------------------------
    // ⭐ 추가된 메소드: 서버로부터 닉네임 + 승률 전달받아 갱신
    // ---------------------------------------------
    public void updatePlayerInfo(String blackName, int blackWin, int blackLose, double blackRate,
                                 String whiteName, int whiteWin, int whiteLose, double whiteRate) {

        this.blackInfo = blackName + " " + blackWin + "승 " + blackLose + "패 (" + String.format("%.0f%%", blackRate) + ")";
        this.whiteInfo = whiteName + " " + whiteWin + "승 " + whiteLose + "패 (" + String.format("%.0f%%", whiteRate) + ")";
        repaint();
    }
    // ---------------------------------------------

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 배경색
        g.setColor(new Color(240, 200, 120));
        g.fillRect(0, 0, getWidth(), getHeight());

        // ---------------------------------------------
        // ⭐ 추가된 UI 요소: 닉네임 + 승률 표시
        // ---------------------------------------------
        g.setColor(Color.BLACK);
        g.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        g.drawString("흑: " + blackInfo, 30, 20);
        g.drawString("백: " + whiteInfo, 250, 20);
        // ---------------------------------------------


        // 격자
        g.setColor(Color.BLACK);
        for (int i = 0; i < 15; i++) {
            g.drawLine(30, 30 + i * 30, 450, 30 + i * 30);
            g.drawLine(30 + i * 30, 30, 30 + i * 30, 450);
        }

        //천원(중점)
        g.fillOval(235,235,10,10);

        // 돌
        for (int i = 0; i < 15; i++)
            for (int j = 0; j < 15; j++) {
                if (board[i][j] == 1) {
                    g.setColor(Color.BLACK);
                    g.fillOval(i * 30 + 20, j * 30 + 20, 20, 20);
                } else if (board[i][j] == 2) {
                    g.setColor(Color.WHITE);
                    g.fillOval(i * 30 + 20, j * 30 + 20, 20, 20);
                }
            }
        
        // 마우스 호버 위치에 반투명한 미리보기 돌 표시
        if (hoverX >= 0 && hoverY >= 0 && hoverX < 15 && hoverY < 15 && board[hoverX][hoverY] == 0) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 금수 여부 확인
            boolean isForbidden = GameRule.isForbidden(board, hoverX, hoverY, playerId);
            
            // 플레이어의 색상으로 반투명 돌 그리기 (투명도 약 50%)
            if (playerId == 1) {
                if (isForbidden) {
                    g2d.setColor(new Color(255, 0, 0, 128)); // 금수면 빨간색
                } else {
                    g2d.setColor(new Color(0, 0, 0, 128)); // 검은색, 반투명
                }
            } else {
                g2d.setColor(new Color(255, 255, 255, 128)); // 흰색, 반투명
            }
            g2d.fillOval(hoverX * 30 + 20, hoverY * 30 + 20, 20, 20);
            
            // 금수일 경우 텍스트 표시
            if (isForbidden) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("", Font.BOLD, 12));
                g2d.drawString("금수", hoverX * 30 + 15, hoverY * 30 + 35);
            }
        }
    }

    public void updateBoard(int x, int y, int player) {
        board[x][y] = player;
        repaint();
        if (GameRule.isDraw(board)) {
            showGameEndDialog("무승부입니다!");
        }
    }

    /**
     * 게임 종료 시 "다시하기"와 "나가기" 버튼이 있는 dialog를 표시합니다.
     */
    private void showGameEndDialog(String message) {
        int option = JOptionPane.showOptionDialog(
            this,
            message,
            "게임 종료",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[]{"다시하기", "나가기"},
            "다시하기"
        );
        
        if (option == JOptionPane.YES_OPTION) {
            // 다시하기: 서버에 RESET 메시지 전송
            network.sendReset();
        } else {
            // 나가기: 게임 종료
            System.exit(0);
        }
    }

    /**
     * 보드를 초기 상태로 리셋합니다.
     */
    private void resetBoard() {
        board = new int[15][15];
        hoverX = -1;
        hoverY = -1;
        repaint();
    }

    /**
     * 서버로부터 받은 승리 신호를 처리합니다 (승리/무승부 dialog 표시).
     */
    /**
     * 서버로부터 받은 승리 신호를 처리합니다 (승리/무승부 dialog 표시).
     */
    public void handleWin(int winner) {
        showGameEndDialog("🎉" + winner + " 승리!");
    }

    /**
     * 서버로부터 받은 게임 초기화 신호를 처리합니다 (보드 리셋만 수행).
     */
    public void handleReset() {
        resetBoard();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = (e.getX() - 30) / 30;
        int y = (e.getY() - 30) / 30;
        // 유효 좌표인지, 비어있는 칸인지, 금수가 아닌지 확인한 뒤 서버로 전송
        if (x >= 0 && y >= 0 && x < 15 && y < 15 && board[x][y] == 0) {
            if (GameRule.isForbidden(board, x, y, playerId)) {
                JOptionPane.showMessageDialog(this, "금수입니다! 다른 위치에 두세요.");
            } else {
                network.sendMove(x, y);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int x = (e.getX() - 30) / 30;
        int y = (e.getY() - 30) / 30;
        // 유효한 범위 내에 있으면 호버 위치 업데이트
        if (x >= 0 && y >= 0 && x < 15 && y < 15) {
            hoverX = x;
            hoverY = y;
        } else {
            hoverX = -1;
            hoverY = -1;
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // 드래그 중에도 호버 위치 추적
        mouseMoved(e);
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {
        // 마우스가 패널을 떠나면 호버 위치 초기화
        hoverX = -1;
        hoverY = -1;
        repaint();
    }
}
