package tictac;
/* 
Created by: Kubilay Sarıbardak
CSE102 TicTacToe Project
*/
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.sun.rowset.CachedRowSetImpl;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class tictactoe implements Runnable {

	private String ip = "localhost";
	private int port = 22222;
	private Scanner scanner = new Scanner(System.in);
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 527;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;

	private ServerSocket serverSocket;

	private BufferedImage board;
	private BufferedImage redX;
	private BufferedImage blueX;
	private BufferedImage redCircle;
	private BufferedImage blueCircle;

	private String[] spaces = new String[9];

	private boolean yourTurn = false;
	private boolean circle = true;
	private boolean accepted = false;
	private boolean unableToCommunicateWithOpponent = false;
	private boolean won = false;
	private boolean enemyWon = false;
	private boolean tie = false;

	private int lengthOfSpace = 160;
	private int errors = 0;
	private int firstSpot = -1;
	private int secondSpot = -1;

	private Font font = new Font("Verdana", Font.BOLD, 32);
	private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
	private Font largerFont = new Font("Verdana", Font.BOLD, 50);

	private String waitingString = "Waiting for another player";
	private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
	private String wonString = "You won!";
	private String enemyWonString = "Opponent won!";
	private String tieString = "A Tie.";

        private Player player = null; 
        
	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };

	public tictactoe() {
		
            /* 
            If you want to write manual ip and port:
                
                System.out.println("Please input the IP: ");
		ip = scanner.nextLine();
		System.out.println("Please input the port: ");
		port = scanner.nextInt();
		while (port < 1 || port > 65535) {
			System.out.println("The port you entered was invalid, please input another port: ");
			port = scanner.nextInt();
		}
            */
                
                System.out.println("Please input the UserName: ");
		String name = scanner.nextLine();
		System.out.println("Please input the Password: ");
		String pass = scanner.nextLine();
		
                
            
            try {
                DBConn.dbConnect();
                player = getuser(name,pass);
                System.out.println(player.getID());
                
                
            } catch (SQLException ex) {
                Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
            }
            
		loadImages();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		if (!connect()) initializeServer();

		frame = new JFrame();
		frame.setTitle("Tic-Tac-Toe");
		frame.setContentPane(painter);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);

		thread = new Thread(this, "TicTacToe");
		thread.start();
	}

        @Override
	public void run() {
		while (true) {
			tick();
			painter.repaint();

			if (!circle && !accepted) {
				listenForServerRequest();
			}

		}
	}

	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if (unableToCommunicateWithOpponent) {
			g.setColor(Color.RED);
			g.setFont(smallerFont);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
			g.drawString(unableToCommunicateWithOpponentString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
			return;
		}

		if (accepted) {
			for (int i = 0; i < spaces.length; i++) {
				if (spaces[i] != null) {
					if (spaces[i].equals("X")) {
						if (circle) {
							g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					} else if (spaces[i].equals("O")) {
						if (circle) {
							g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (won || enemyWon) {
                                int id = player.getID();
                                int win = player.getWin();
                                int lose = player.getLose();
                                int score = player.getScore();
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.BLACK);
				g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);

				g.setColor(Color.RED);
				g.setFont(largerFont);
				if (won) {
                                    try {
                                        DBConn.dbExecuteUpdate("UPDATE Conn Set UScore = " + (++score) + ", UWin = " + (++win) + " Where ID = " + id + ";");
                                    } catch (SQLException ex) {
                                        Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ClassNotFoundException ex) {
                                        Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                        
					int stringWidth = g2.getFontMetrics().stringWidth(wonString);
					g.drawString(wonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
				} else if (enemyWon) {
                                        try {
                                        DBConn.dbExecuteUpdate("UPDATE Conn Set ULose = " + (++lose) + ", UScore = " + (--score) + " Where ID = " + id + ";");
                                    } catch (SQLException ex) {
                                        Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ClassNotFoundException ex) {
                                        Logger.getLogger(tictactoe.class.getName()).log(Level.SEVERE, null, ex);
                                    }
					int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
					g.drawString(enemyWonString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
				}
			}
			if (tie) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(largerFont);
				int stringWidth = g2.getFontMetrics().stringWidth(tieString);
				g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
			}
		} else {
			g.setColor(Color.RED);
			g.setFont(font);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
			g.drawString(waitingString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
		}

	}

	private void tick() {
		if (errors >= 10) unableToCommunicateWithOpponent = true;

		if (!yourTurn && !unableToCommunicateWithOpponent) {
			try {
				int space = dis.readInt();
				if (circle) spaces[space] = "X";
				else spaces[space] = "O";
				checkForEnemyWin();
				checkForTie();
				yourTurn = true;
			} catch (IOException e) {
				e.printStackTrace();
				errors++;
			}
		}
	}

	private void checkForWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			} else {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			}
		}
	}

	private void checkForEnemyWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			} else {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			}
		}
	}

	private void checkForTie() {
		for (int i = 0; i < spaces.length; i++) {
			if (spaces[i] == null) {
				return;
			}
		}
		tie = true;
	}

	private void listenForServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
			System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean connect() {
		try {
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
		} catch (IOException e) {
			System.out.println("Unable to connect to the address: " + ip + ":" + port + " | Starting a server");
			return false;
		}
		System.out.println("Successfully connected to the server.");
		return true;
	}

	private void initializeServer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
		circle = false;
	}

	private void loadImages() {
		try {
                        board = ImageIO.read(new File(".\\res\\board.png"));
			
                        redX = ImageIO.read(new File(".\\res\\redX.png"));
			redCircle = ImageIO.read(new File(".\\res\\redCircle.png"));
			blueX = ImageIO.read(new File(".\\res\\blueX.png"));
			blueCircle = ImageIO.read(new File(".\\res\\blueCircle.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		tictactoe ticTacToe = new tictactoe();
	}

    private Player getuser(String name, String pass) throws ClassNotFoundException, SQLException {
       String query = "Select * from Conn where UName = '" + name  + "' AND UPass = '" + pass + "';" ;
        try {
            
            ResultSet rsPer = DBConn.dbExecuteQuery(query);
            Player person = getPersonFromResultSet(rsPer);
 
            
            return person;
        } catch (SQLException e) {
            System.out.println("While searching an person with");
         
            throw e;
        }
    }
    
    private static Player getPersonFromResultSet(ResultSet rs) throws SQLException
    {
        Player per = null;
        if (rs.next()) {
            per = new Player();
            per.setID(rs.getInt("ID"));
            per.setWin(rs.getInt("UWin"));
            per.setLose(rs.getInt("ULose"));
            per.setScore(rs.getInt("UScore"));
}
      
        return per;
    }

private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (accepted) {
				if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
					int x = e.getX() / lengthOfSpace;
					int y = e.getY() / lengthOfSpace;
					y *= 3;
					int position = x + y;

					if (spaces[position] == null) {
						if (!circle) spaces[position] = "X";
						else spaces[position] = "O";
						yourTurn = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(position);
							dos.flush();
						} catch (IOException e1) {
							errors++;
							e1.printStackTrace();
						}

						System.out.println("DATA WAS SENT");
						checkForWin();
						checkForTie();

					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}

class DBConn{

    private static final String DRIVER = "com.mysql.jdbc.Driver";
    private static Connection conn = null;
    // jdbc:mysql://hostname:port/database?useUnicode=true&characterEncoding=utf8
    private static final String URL = "jdbc:mysql://127.0.0.1/tictactoe?useUnicode=true&characterEncoding=utf8";
    private static final String USER = "kybe";
    private static final String PASSWORD = "123456789";
    static Statement stmt = null;

    public static void dbConnect() throws SQLException, ClassNotFoundException {
        try {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            
        } 
        
        catch (SQLException e) {
            System.out.println("Connection Failed! Check output console" + e);
            e.printStackTrace();
            throw e;
        }
    }
 
    public static void dbDisconnect() throws SQLException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e){
           throw e;
        }
    }
 

    public static ResultSet dbExecuteQuery(String queryStmt) throws SQLException, ClassNotFoundException {
        
        ResultSet resultSet = null;
        CachedRowSetImpl crs = null;
        try {
            dbConnect();
            System.out.println("Select statement: " + queryStmt + "\n");
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(queryStmt);
            crs = new CachedRowSetImpl();
            crs.populate(resultSet);
        } catch (SQLException e) {
            System.out.println("Problem occurred at executeQuery operation : " + e);
            throw e;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            dbDisconnect();
        }
        return crs;
    }
    public static void dbExecuteUpdate(String sqlStmt) throws SQLException, ClassNotFoundException {
        try {
            dbConnect();
            stmt = conn.createStatement();
            stmt.executeUpdate(sqlStmt);
        } catch (SQLException e) {
            System.out.println("Problem occurred at execute Update operation : " + e);
            throw e;
        } finally {
            if (stmt != null) {
                stmt.close();
            }
            dbDisconnect();
        }

    }

}

class Player{
    int ID;
    int Score;
    int Win;
    int Lose;

    public int getScore() {
        return Score;
    }

    public void setScore(int Score) {
        this.Score = Score;
    }

    public int getWin() {
        return Win;
    }

    public void setWin(int Win) {
        this.Win = Win;
    }

    public int getLose() {
        return Lose;
    }

    public void setLose(int Lose) {
        this.Lose = Lose;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

}