import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.sound.sampled.AudioSystem
import javax.swing.*
import javax.swing.Timer
import kotlin.math.min

class TicTacToe(private val statusLabel: JLabel) : JPanel() {
    private val board = Array(3) { Array(3) { "" } }
    private var currentPlayer = "X"
    private var winner: String? = null
    private var winningLine: Triple<Point, Point, Point>? = null
    private var flashAlpha = 1.0f
    private var flashTimer: Timer? = null
    private var gameEnded = false

    init {
        background = Color(240, 248, 255)
        updateStatusLabel()

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (gameEnded) return
                val cellSize = min(width, height) / 3
                val xOffset = (width - cellSize * 3) / 2
                val yOffset = (height - cellSize * 3) / 2
                val col = (e.x - xOffset) / cellSize
                val row = (e.y - yOffset) / cellSize
                if (row in 0..2 && col in 0..2 && board[row][col].isEmpty()) {
                    playSound("assets/click.wav")
                    board[row][col] = currentPlayer
                    checkGameState()
                    if (!gameEnded) {
                        currentPlayer = if (currentPlayer == "X") "O" else "X"
                        updateStatusLabel()
                    }
                    repaint()
                }
            }
        })
    }

    private fun updateStatusLabel() {
        statusLabel.text = if (gameEnded) {
            if (winner != null) "Player $winner wins!" else "It's a draw!"
        } else {
            "Player $currentPlayer's turn"
        }

        statusLabel.foreground = when {
            gameEnded && winner != null -> Color(255, 215, 0) // Gold
            gameEnded && winner == null -> Color(30, 144, 255) // Blue
            currentPlayer == "X" -> Color(0, 150, 136) // Teal
            else -> Color(244, 67, 54) // Coral
        }
    }

    fun resetGame() {
        for (r in 0..2) for (c in 0..2) board[r][c] = ""
        currentPlayer = "X"
        winner = null
        winningLine = null
        gameEnded = false
        flashTimer?.stop()
        flashAlpha = 1.0f
        updateStatusLabel()
        repaint()
    }

    private fun playSound(path: String) {
        try {
            val file = File(path)
            val audioIn = AudioSystem.getAudioInputStream(file)
            val clip = AudioSystem.getClip()
            clip.open(audioIn)
            clip.start()
        } catch (e: Exception) {
            println("Error playing sound: ${e.message}")
        }
    }

    private fun checkGameState() {
        val lines = listOf(
            Triple(Point(0, 0), Point(0, 1), Point(0, 2)),
            Triple(Point(1, 0), Point(1, 1), Point(1, 2)),
            Triple(Point(2, 0), Point(2, 1), Point(2, 2)),
            Triple(Point(0, 0), Point(1, 0), Point(2, 0)),
            Triple(Point(0, 1), Point(1, 1), Point(2, 1)),
            Triple(Point(0, 2), Point(1, 2), Point(2, 2)),
            Triple(Point(0, 0), Point(1, 1), Point(2, 2)),
            Triple(Point(0, 2), Point(1, 1), Point(2, 0))
        )

        for (line in lines) {
            val (a, b, c) = line
            val v1 = board[a.x][a.y]
            val v2 = board[b.x][b.y]
            val v3 = board[c.x][c.y]
            if (v1.isNotEmpty() && v1 == v2 && v2 == v3) {
                winner = v1
                winningLine = line
                gameEnded = true
                playSound("assets/win.wav")
                startFlashAnimation()
                updateStatusLabel()
                return
            }
        }

        if (board.all { row -> row.all { it.isNotEmpty() } }) {
            gameEnded = true
            playSound("assets/draw.wav")
            updateStatusLabel()
        }
    }

    private fun startFlashAnimation() {
        flashTimer = Timer(100) {
            flashAlpha = if (flashAlpha == 1.0f) 0.3f else 1.0f
            repaint()
        }
        flashTimer?.start()

        Timer(2000) {
            flashTimer?.stop()
            flashAlpha = 1.0f
            repaint()
        }.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cellSize = min(width, height) / 3
        val xOffset = (width - cellSize * 3) / 2
        val yOffset = (height - cellSize * 3) / 2

        // Grid
        g2.stroke = BasicStroke(4f)
        g2.color = Color.BLACK
        for (i in 1..2) {
            g2.drawLine(xOffset + i * cellSize, yOffset, xOffset + i * cellSize, yOffset + 3 * cellSize)
            g2.drawLine(xOffset, yOffset + i * cellSize, xOffset + 3 * cellSize, yOffset + i * cellSize)
        }

        // X & O
        for (r in 0..2) {
            for (c in 0..2) {
                val mark = board[r][c]
                if (mark.isNotEmpty()) {
                    val cx = xOffset + c * cellSize
                    val cy = yOffset + r * cellSize
                    val padding = cellSize / 6
                    val markColor = if (winner != null && winningLine?.toList()?.contains(Point(r, c)) == true)
                        Color(255, 215, 0, (flashAlpha * 255).toInt()) // gold flash
                    else if (mark == "X") Color(0, 150, 136)
                    else Color(244, 67, 54)

                    g2.color = markColor
                    g2.stroke = BasicStroke(6f)

                    if (mark == "X") {
                        g2.drawLine(cx + padding, cy + padding, cx + cellSize - padding, cy + cellSize - padding)
                        g2.drawLine(cx + cellSize - padding, cy + padding, cx + padding, cy + cellSize - padding)
                    } else {
                        g2.drawOval(cx + padding, cy + padding, cellSize - 2 * padding, cellSize - 2 * padding)
                    }
                }
            }
        }

        // Strike line
        if (winner != null && winningLine != null) {
            val (a, _, c) = winningLine!!
            g2.color = Color(255, 215, 0)
            g2.stroke = BasicStroke(8f)
            val startX = xOffset + a.y * cellSize + cellSize / 2
            val startY = yOffset + a.x * cellSize + cellSize / 2
            val endX = xOffset + c.y * cellSize + cellSize / 2
            val endY = yOffset + c.x * cellSize + cellSize / 2
            g2.drawLine(startX, startY, endX, endY)
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Tic Tac Toe")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()

        val statusLabel = JLabel("", SwingConstants.CENTER)
        statusLabel.font = Font("Arial", Font.BOLD, 20)
        frame.add(statusLabel, BorderLayout.NORTH)

        val gamePanel = TicTacToe(statusLabel)
        frame.add(gamePanel, BorderLayout.CENTER)

        val resetButton = JButton("Reset Game")
        resetButton.font = Font("Arial", Font.BOLD, 16)
        resetButton.addActionListener { gamePanel.resetGame() }
        frame.add(resetButton, BorderLayout.SOUTH)

        frame.setSize(700, 750)
        frame.isVisible = true
    }
}
