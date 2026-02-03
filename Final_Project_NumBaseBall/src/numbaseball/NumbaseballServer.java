package numbaseball;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class NumbaseballServer extends JFrame {
	
	private int port;
	private ServerSocket serverSocket;
	
	private Thread acceptThread = null;
	private Vector<ClientHandler> users = new Vector<>();
	
	private JTextArea t_display;
	private JButton b_connect, b_disconnect, b_exit, b_send;
	private JTextField messageField;
	
	private int numOfUsersSelected = 0;

	
	public NumbaseballServer(int port) {
		
		super("num Server");
		
		
		buildGui();
		
		setSize(400,300);
		setLocation(700,650);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setVisible(true);
		
		this.port = port;
	}
	
	private void buildGui() {
		add(createDisplayPanel(), BorderLayout.CENTER);
		add(createInputPanel(), BorderLayout.NORTH);

		JPanel p = new JPanel(new GridLayout(1,0));
		//p.add(createInputPanel());
		p.add(createControlPanel());
		add(p, BorderLayout.SOUTH);
	}
	
	// 진행 상황을 기록하는 UI
	private JPanel createDisplayPanel() {
		JPanel p = new JPanel(new BorderLayout());
		
		t_display = new JTextArea();
		t_display.setEditable(false);
		
		p.add(new JScrollPane(t_display), BorderLayout.CENTER);
		
		return p;
	}
	
	// 클라이언트들에게 공지사항을 전송하는 UI
	private JPanel createInputPanel() {
        JPanel p = new JPanel(new GridLayout(1, 0));

        messageField = new JTextField();
        b_send = new JButton("보내기");

        b_send.addActionListener(new ActionListener() {
            
        	@Override
            public void actionPerformed(ActionEvent e) {
                sendMessageFromServer();
            }
        });

        p.add(messageField);
        p.add(b_send);

        return p;
    }
	
	// 서버를 관리하는 UI
	private JPanel createControlPanel() {
		JPanel p = new JPanel(new GridLayout(1,0));
		
		b_connect = new JButton("서버 시작");	
		b_connect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				acceptThread = new Thread(new Runnable() {

					@Override
					public void run() {
						startServer();
					}
				});
				acceptThread.start();
				
				b_connect.setEnabled(false);
				b_disconnect.setEnabled(true);
				b_exit.setEnabled(false);
			}
			
		});
		
		b_disconnect = new JButton("서버 종료");
		b_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
				
				b_connect.setEnabled(true);
				b_disconnect.setEnabled(false);
				
				b_exit.setEnabled(true);
			}
			
		});
		
		b_exit = new JButton("종료하기");
		b_exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
			
		});
		
		p.add(b_connect);
		p.add(b_disconnect);
		p.add(b_exit);
		
		b_disconnect.setEnabled(false);
		
		return p;
	}
	
	// 서버를 실행하는 함수
	private void startServer() {
		Socket clientSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			printDisplay("서버가 시작되었습니다: " + getLocalAddr());
			
			while(acceptThread == Thread.currentThread()) {
				clientSocket = serverSocket.accept();
				//accept 후 생성한 ClientHandler를 users에 저장
				String cAddr = clientSocket.getInetAddress().getHostAddress();
				t_display.append("클라이언트가 연결되었습니다: " + cAddr + "\n");
				
				ClientHandler cHandler = new ClientHandler(clientSocket);
				users.add(cHandler);
				cHandler.start();
				
			}
			} catch (SocketException e) {
				printDisplay("서버 소켓 종료");
			} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (clientSocket!=null) clientSocket.close();
				if(serverSocket!=null) serverSocket.close();
			} catch (IOException e) {
				System.out.println("서버 닫기 오류: " + e.getMessage());
				System.exit(-1);
			}
		}
		
	}
	
	// 서버를 종료하는 함수
	private void disconnect() {
		try {
			acceptThread = null;
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("서버 소켓 닫기 오류 -> " + e.getMessage());
			System.exit(-1);
		}
	}
	
	// 연결된 클라이언트들을 관리하는 함수
	private class ClientHandler extends Thread{
		private Socket clientSocket;
		private ObjectOutputStream out;
		private int[] chooseNumber;
		
		private String uid;
		
		public ClientHandler(Socket clientSocket) {
			this.clientSocket = clientSocket;
			
		}
		
		public int[] getChooseNumber() {
		    return chooseNumber;
		}
		

		private void receiveMessages(Socket cs) {
		    try {
		    	ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(cs.getInputStream()));
		    	out = new ObjectOutputStream(new BufferedOutputStream(cs.getOutputStream()));
		    		    	
		    	String message;

		    	ChatMsg msg;
		        while ((msg =(ChatMsg)in.readObject()) != null) {
		        	if(msg.mode == ChatMsg.MODE_LOGIN) {
		        		uid = msg.userID;
		        		
		        		printDisplay("새 참가자: "+ uid);
		        		printDisplay("현재 참가자 수: " + users.size());
		        		if(users.size() == 2) {
		    		    	String userCome = "유저가 모두 참가하였습니다!\n" + "         4자리 숫자를 지정해주세요!";
		        			ChatMsg gameStartAlarm = new ChatMsg("서버", ChatMsg.MODE_TX_STRING, userCome);
		        			broadcasting(gameStartAlarm);
		        		}
		        		continue;
		        		}
		        	else if (msg.mode == ChatMsg.MODE_LOGOUT) {
		        		break;
		        	}
		        	else if(msg.mode == ChatMsg.MODE_TX_STRING) {

			        	message = uid + ": " + msg.message;
			        	
			        	printDisplay(message);
			        	broadcasting(msg);
		        	}
		        	else if(msg.mode == ChatMsg.MODE_Choose_NUM) {
		        	    chooseNumber = msg.chooseNumber;
		        	    message = uid + "(이)가 고른 숫자: " + Arrays.toString(chooseNumber);
		        	    
		        	    String doneChoose = uid + "(이)가 숫자 고르기를 완료했습니다!\n";
		        	    String attackFirst = "          " + uid + "(이)가 선공입니다.";
		        	    String attackLater = "          " + uid + "(이)가 후공입니다.";
		        	    if(numOfUsersSelected == 0) {
		        	    	ChatMsg Alldone = new ChatMsg("서버", ChatMsg.MODE_TX_STRING, doneChoose+attackFirst);
			        	    printDisplay(message);
			        	    broadcasting(Alldone);
		        	    }
		        	    
		        	    if(numOfUsersSelected == 1) {
		        	    	ChatMsg Alldone = new ChatMsg("서버", ChatMsg.MODE_TX_STRING, doneChoose+attackLater);
			        	    printDisplay(message);
			        	    broadcasting(Alldone);
		        	    }
		        	    
		        	    numOfUsersSelected++;  // 숫자를 선택한 사용자 수 증가

		        	    // 모든 사용자가 숫자를 선택했는지 확인
		        	    if (numOfUsersSelected == 2) {
		        	        String gameStartMsg = "두 유저가 모두 숫자 고르기를 마쳤습니다.\n" + "                       게임을 시작합니다.";
		        	        ChatMsg gameStart = new ChatMsg("서버", ChatMsg.MODE_TX_STRING, gameStartMsg);
		        	        broadcasting(gameStart);
		        	        numOfUsersSelected = 0;
		        	    }
		        	}
		        	else if(msg.mode == ChatMsg.MODE_Guess_NUM) {
		        	    int guessNumber = msg.guessNumber;
		        	    String compareResult = NumbaseballServer.this.compareNumbers(guessNumber, this);
		        	    String result = uid + "(이)가 추리한 숫자: " + guessNumber;
		        	    String turnAlarm = "'s Turn Over!";
		        	    String gameOver = uid + "(이)가 숫자를 맞췄습니다.\n" + uid + "의 승리!";
		        	    printDisplay(result);
		        	    printDisplay(compareResult);
		        	    
		        	    msg.compareResult = compareResult;
		        	    
		        	    if(compareResult != "4S") {
			        	    broadcasting(result +": " + compareResult);
			        	    broadcasting(uid + turnAlarm);
			        	    broadcastToClient(msg, this);
		        	    }
		        	    else {
		        	    	broadcasting(gameOver);
		        	    }
		        	}
		        	else if(msg.mode == ChatMsg.MODE_3_Hint) {
		        		hint3(this);
		        	}
		        	else if(msg.mode == ChatMsg.MODE_4_Hint) {
		        		hint4(this);
		        	}
		        	else if(msg.mode == ChatMsg.MODE_5_Hint) {
		        		hint5(this);
		        	}
		        	else if(msg.mode == ChatMsg.MODE_6_Hint) {
		        		hint6(this);
		        	}
		        	else if(msg.mode == ChatMsg.MODE_7_Hint) {
		        		hint7();
		        	}
		        }
		        
		        users.removeElement(this);
		        printDisplay(uid + " 퇴장, 현재 참가자 수 : " + users.size());
			}catch(IOException | ClassNotFoundException e){
				users.removeElement(this);
				printDisplay(uid + " 연결 끊김. 현재 참가자 수: " + users.size());
			}
			finally {
				try {
					cs.close();
				}catch(IOException e){
					System.err.println("서버 닫기 오류: " + e.getMessage());
					System.exit(-1);
				}
			}
		}
		// 힌트 7에 대한 함수
		private void hint7() {
		    for (ClientHandler c : users) {
		        if (c != this) { 

		        	String message = "아이템 사용: 다른 사용자의 데이터 패널 초기화";
		            ChatMsg msg = new ChatMsg(uid, ChatMsg.MODE_7_Hint, message);
		            // 메시지 전송
		            broadcastToClient(msg, c);
		        }
		    }
		}
		
		
		private void send(ChatMsg msg) {
			try {
				out.writeObject(msg);
				out.flush();
			} catch (IOException e) {
				System.err.println("클라이언트 일반 전송 오류 -> " + e.getMessage());
			}
		}
		
		
	    private void sendMessage(String msg) {
	    	send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, msg));
	    }
		
		private void broadcasting(ChatMsg msg) {
			for(ClientHandler c: users) {
				c.send(msg);
			}
		}
		
		private void broadcasting(String msg) {
			for(ClientHandler c: users) {
				c.sendMessage(msg);
			}
		}
		
		@Override
		public void run() {
			receiveMessages(clientSocket);
		}
	}
	
    // 숫자를 채점해주는 함수
	private String compareNumbers(int guessNumber, ClientHandler guesser) {
	    int[] guessNumberArray = Integer.toString(guessNumber).chars().map(c -> c-'0').toArray();
	    for (ClientHandler c : users) {
	        if (c != guesser) { // 자기 자신의 숫자는 제외
	            int[] chooseNumber = c.getChooseNumber();
	            
	            int strike = 0;
	            int ball = 0;

	            // 각 자리수를 비교
	            for (int i = 0; i < guessNumberArray.length; i++) {
	                for (int j = 0; j < chooseNumber.length; j++) {
	                    if (guessNumberArray[i] == chooseNumber[j]) {
	                        if (i == j) { // 위치도 같으면 스트라이크
	                            strike++;
	                        } else { // 위치는 다르지만 숫자가 같으면 볼
	                            ball++;
	                        }
	                    }
	                }
	            }

	            if (strike == guessNumberArray.length) { // 모든 숫자가 맞았다면
	                return "4S";
	            }
	            else if(strike == 0 && ball == 0)	return "아웃!";
	            else if(strike == 0) return ball + "B ";
	            else if(ball == 0) return strike + "S ";
	            else return strike + "S " + ball + "B "; 
	        }
	    }
	    return "아직 맞추지 못했습니다.";
	}

	// 힌트 3에 대한 함수
	private void hint3(ClientHandler requester) {
	    for (ClientHandler c : users) {
	        if (c != requester) {
	            int[] chooseNumber = c.getChooseNumber();

	            int hint = chooseNumber[0];
	            
	            String hintMessage = "힌트: " + c.uid + "의 첫 번째 숫자는 " + hint + "입니다.";
	            ChatMsg hintMsg = new ChatMsg(requester.uid, ChatMsg.MODE_3_Hint, hintMessage);
	            hintMsg.hintMessage = hintMessage;
	            broadcastToClient(hintMsg, requester);
	        }
	    }
	}
	
	// 힌트 4에 대한 함수
	private void hint4(ClientHandler requester) {
	    for (ClientHandler c : users) {
	        if (c != requester) {
	            int[] chooseNumber = c.getChooseNumber();
	            
	            int hint = chooseNumber[1];
	 
	            String hintMessage = "힌트: " + c.uid + "의 두 번째 숫자는 " + hint + "입니다.";
	            ChatMsg hintMsg = new ChatMsg(requester.uid, ChatMsg.MODE_4_Hint, hintMessage);
	            hintMsg.hintMessage = hintMessage;
	            broadcastToClient(hintMsg, requester);
	        }
	    }
	}
	
	// 힌트 5에 대한 함수
	private void hint5(ClientHandler requester) {
	    for (ClientHandler c : users) {
	        if (c != requester) { 
	            int[] chooseNumber = c.getChooseNumber();
	            
	            int hint = chooseNumber[2];
	            
	            String hintMessage = "힌트: " + c.uid + "의 세 번째 숫자는 " + hint + "입니다.";

	            ChatMsg hintMsg = new ChatMsg(requester.uid, ChatMsg.MODE_5_Hint, hintMessage);
	            hintMsg.hintMessage = hintMessage;
	            broadcastToClient(hintMsg, requester);
	        }
	    }
	}
	
	// 힌트 6에 대한 함수
	private void hint6(ClientHandler requester) {
	    for (ClientHandler c : users) {
	        if (c != requester) {
	            int[] chooseNumber = c.getChooseNumber();

	            int hint = chooseNumber[3];
	            
	            String hintMessage = "힌트: " + c.uid + "의 네 번째 숫자는 " + hint + "입니다.";
	            
	            ChatMsg hintMsg = new ChatMsg(requester.uid, ChatMsg.MODE_6_Hint, hintMessage);
	            hintMsg.hintMessage = hintMessage;
	            broadcastToClient(hintMsg, requester);
	        }
	    }
	}
	
	
    public void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }
    

    private void sendMessageFromServer() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
        	sendServerMessage(message);
        	printDisplay("공지: " + message);
            messageField.setText("");
        }
    }
    
    private void sendServerMessage(String message) {
        ChatMsg serverMessage = new ChatMsg("서버", ChatMsg.MODE_TX_STRING, message);
        broadcasting(serverMessage);
    }
    
    private void broadcastToClient(ChatMsg msg, ClientHandler clientHandler) {
        clientHandler.send(msg);
    }

    private void broadcasting(ChatMsg msg) {
        for (ClientHandler c : users) {
            broadcastToClient(msg, c);
        }
    }
    
	private String getLocalAddr() {
		InetAddress local = null;
		String addr = "";
		try {
			local = InetAddress.getLocalHost();
			addr = local.getHostAddress();
			System.out.println(addr);
		} catch (UnknownHostException e) {
		}
		return addr;
	}
	
	
	public static void main(String[] args) {
		int port = 54321;
		
		new NumbaseballServer(port);

	}
}