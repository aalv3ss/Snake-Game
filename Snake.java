import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.*;

/**
 * Snake Arcade Game em Java
 */
public class SnakeGame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Arcade");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GamePanel panel = new GamePanel();
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Dimensões do tabuleiro
    private final int WIDTH = 600;
    private final int HEIGHT = 600;
    private final int UNIT = 20;

    // Dados do jogo
    private ArrayList<BodyPart> snake;
    private Food food;
    private java.util.List<Obstacle> obstacles = new ArrayList<>();
    private Random rand = new Random();

    // Estado
    private boolean running = true;
    private int direction = KeyEvent.VK_RIGHT;

    // Timer (velocidade)
    private Timer timer;
    private int speed = 150;

    // Pontuação e níveis
    private int score = 0;
    private int level = 1;

    // Highscore guardado em ficheiro txt
    private int highscore = 0;
    private final String HS_FILE = "highscore.txt";

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        snake = new ArrayList<>();
        snake.add(new BodyPart(5 * UNIT, 5 * UNIT));

        loadHighscore();
        spawnFood();

        timer = new Timer(speed, this);
        timer.start();
    }

    /* ===================== RENDER ===================== */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (running) draw(g);
        else drawGameOver(g);
    }

    private void draw(Graphics g) {

        // Comida com outline
        g.setColor(Color.RED);
        g.fillOval(food.x, food.y, UNIT, UNIT);
        g.setColor(Color.WHITE);
        g.drawOval(food.x, food.y, UNIT, UNIT);

        // Obstáculos (níveis)
        g.setColor(Color.GRAY);
        for (Obstacle o : obstacles) g.fillRect(o.x, o.y, UNIT, UNIT);

        // Cobra
        for (int i = 0; i < snake.size(); i++) {
            g.setColor(i == 0 ? Color.YELLOW : Color.GREEN);
            g.fillRect(snake.get(i).x, snake.get(i).y, UNIT, UNIT);
        }

        // UI
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 20);
        g.drawString("Highscore: " + highscore, 10, 40);
        g.drawString("Nível: " + level, WIDTH - 120, 20);
    }

    /* ===================== GAME LOGIC ===================== */

    private void move() {
        for (int i = snake.size() - 1; i > 0; i--) {
            snake.get(i).x = snake.get(i - 1).x;
            snake.get(i).y = snake.get(i - 1).y;
        }

        switch (direction) {
            case KeyEvent.VK_UP -> snake.get(0).y -= UNIT;
            case KeyEvent.VK_DOWN -> snake.get(0).y += UNIT;
            case KeyEvent.VK_LEFT -> snake.get(0).x -= UNIT;
            case KeyEvent.VK_RIGHT -> snake.get(0).x += UNIT;
        }
    }

    private void checkCollision() {
        BodyPart head = snake.get(0);

        // Parede
        if (head.x < 0 || head.x >= WIDTH || head.y < 0 || head.y >= HEIGHT) {
            gameOver();
            return;
        }

        // Corpo
        for (int i = 1; i < snake.size(); i++) {
            if (head.x == snake.get(i).x && head.y == snake.get(i).y) {
                gameOver();
                return;
            }
        }

        // Obstáculos
        for (Obstacle o : obstacles) {
            if (head.x == o.x && head.y == o.y) {
                gameOver();
                return;
            }
        }

        // Comer comida
        if (head.x == food.x && head.y == food.y) {
            snake.add(new BodyPart(-UNIT, -UNIT));
            score++;
            playEatSound();
            spawnFood();
            updateLevel();
        }
    }

    private void updateLevel() {
        if (score == 6) setLevel(2);
        if (score == 12) setLevel(3);
    }

    private void setLevel(int lvl) {
        level = lvl;

        // Aumenta velocidade
        if (speed > 50) {
            speed -= 40;
            timer.setDelay(speed);
        }

        // Obstáculos só a partir do nível 2
        if (level >= 2) spawnObstacles(4 + level);
    }

    private void spawnFood() {
        int x, y;
        do {
            x = rand.nextInt(WIDTH / UNIT) * UNIT;
            y = rand.nextInt(HEIGHT / UNIT) * UNIT;
        } while (obstacles.stream().anyMatch(o -> o.x == x && o.y == y));
        food = new Food(x, y);
    }

    private void spawnObstacles(int n) {
        obstacles.clear();
        for (int i = 0; i < n; i++) {
            obstacles.add(new Obstacle(
                    rand.nextInt(WIDTH / UNIT) * UNIT,
                    rand.nextInt(HEIGHT / UNIT) * UNIT
            ));
        }
    }

    /* ===================== GAME OVER ===================== */

    private void gameOver() {
        running = false;
        playGameOverSound();

        // Atualiza highscore
        if (score > highscore) {
            highscore = score;
            saveHighscore();
        }
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        String msg = "GAME OVER";
        g.drawString(msg, (WIDTH - fm.stringWidth(msg)) / 2, HEIGHT / 2);

        g.setFont(new Font("Consolas", Font.BOLD, 25));
        g.drawString("Score: " + score, WIDTH/2 - 50, HEIGHT/2 + 40);
        g.drawString("Highscore: " + highscore, WIDTH/2 - 80, HEIGHT/2 + 80);
    }

    /* ===================== HIGHSCORE FILE ===================== */

    private void loadHighscore() {
        try {
            File f = new File(HS_FILE);
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                highscore = Integer.parseInt(br.readLine().trim());
                br.close();
            }
        } catch (Exception ignored) {}
    }

    private void saveHighscore() {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(HS_FILE));
            pw.println(highscore);
            pw.close();
        } catch (Exception ignored) {}
    }

    /* ===================== TIMER ===================== */

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkCollision();
        }
        repaint();
    }

    /* ===================== INPUT ===================== */

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if ((k == KeyEvent.VK_UP && direction != KeyEvent.VK_DOWN)
                || (k == KeyEvent.VK_DOWN && direction != KeyEvent.VK_UP)
                || (k == KeyEvent.VK_LEFT && direction != KeyEvent.VK_RIGHT)
                || (k == KeyEvent.VK_RIGHT && direction != KeyEvent.VK_LEFT)) {
            direction = k;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    /* ===================== SONS ARCADE ===================== */

    private void playEatSound() {
        new Thread(() -> { try { Tone.play(800, 70); } catch (Exception ignored) {} }).start();
    }

    private void playGameOverSound() {
        new Thread(() -> { try { Tone.play(200, 500); } catch (Exception ignored) {} }).start();
    }
}

/* ===================== OBJETOS ===================== */

class BodyPart { int x,y; BodyPart(int x,int y){this.x=x;this.y=y;} }
class Food      { int x,y; Food(int x,int y){this.x=x;this.y=y;} }
class Obstacle  { int x,y; Obstacle(int x,int y){this.x=x;this.y=y;} }

/* ===================== Tom do SOM ARCADE ===================== */

class Tone {
    public static void play(int freq, int duration) throws LineUnavailableException {
        float sr = 44100;
        byte[] buf = new byte[1];
        AudioFormat af = new AudioFormat(sr, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af); sdl.start();
        for (int i = 0; i < duration * (sr / 1000); i++) {
            double angle = i / (sr / freq) * 2.0 * Math.PI;
            buf[0] = (byte)(Math.sin(angle) * 127f);
            sdl.write(buf, 0, 1);
        }
        sdl.drain(); sdl.stop(); sdl.close();
    }
}