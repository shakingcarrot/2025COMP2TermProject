import javax.swing.*;
import java.awt.*;

/**
 * TimerPanel
 * - 게임의 현재 턴과 남은 시간을 표시하는 패널
 * - 각 플레이어의 턴마다 35초 카운트다운을 표시한다.
 */
public class TimerPanel extends JPanel {
    private int currentPlayer = 1;
    private int remainingTime = 35;

    public TimerPanel() {
        setPreferredSize(new Dimension(500, 60));
        setBackground(new Color(200, 200, 200));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        
        String playerColor = (currentPlayer == 1) ? "흑(1)" : "백(2)";
        String timeText = String.format("현재 턴: %s | 남은 시간: %d초", playerColor, remainingTime);
        
        g.drawString(timeText, 20, 45);
        
        // 시간이 10초 이하일 때 경고 (빨간색)
        if (remainingTime <= 10 && remainingTime > 0) {
            g.setColor(Color.RED);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString(timeText, 20, 45);
        }
    }

    /**
     * 남은 시간을 업데이트하고 화면을 다시 그린다.
     */
    public void updateTime(int seconds) {
        this.remainingTime = seconds;
        repaint();
    }

    /**
     * 현재 플레이어를 설정한다.
     */
    public void setCurrentPlayer(int player) {
        this.currentPlayer = player;
        repaint();
    }
}
