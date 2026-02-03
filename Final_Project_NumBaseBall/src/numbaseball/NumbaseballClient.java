package numbaseball;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

public class NumbaseballClient extends JFrame{
	private JTextField t_input, t_userID, t_hostAddr, t_portNum, t_combo, t_chat;
	private JTextPane t_numberData, t_resultData, t_chatting;
	private JScrollPane ChattingScroll, DataScroll, ResultScroll, imageScroll;
	private DefaultStyledDocument document1, document2, document3;
	private JButton b_send, b_connect, b_exit, b_disconnect, b_guessNum, b_talk, b_combo;
	private JPanel imagePanel;
	
	private Socket socket;
	private String serverAddress;
	private int serverPort;
	
	private ObjectOutputStream out;
	private Thread receiveThread = null;
	
	private String uid;
	private int combo = 0;
	private int NUM_MAX_LENGTH = 4;
	
	
	public NumbaseballClient(String serverAddress, int serverPort) {
		super("Playing Numbaseball");

		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		buildGUI();

		setSize(700, 600);
		setLocation(100,50);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setVisible(true);
	}
	
	private void buildGUI() {
		 JPanel p_user = new JPanel(new BorderLayout());

		 JPanel userPanel = new JPanel(new GridLayout(1, 0));
		 userPanel.add(createUserPanel());
		 userPanel.add(createComboPanel());
		 p_user.add(userPanel, BorderLayout.NORTH);
		
		 JPanel p_center = new JPanel(new GridLayout(1, 0));
		 p_center.add(createDataPanel());
		 p_center.add(createChattingPanel());
		
		add(p_user, BorderLayout.NORTH);
		add(p_center, BorderLayout.CENTER);
		
		
	    JPanel p_input = new JPanel(new GridLayout(1, 0));
	    p_input.add(createInfoPanel());
	    add(p_input, BorderLayout.SOUTH);
		
	}
	
	// 숫자를 정하는 기능을 하는 UI
	private JPanel createUserPanel() {
		JPanel p = new JPanel(new FlowLayout());
		
		t_input = new JTextField();
		
		b_send = new JButton("숫자 정하기");
		b_send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chooseNumber();
			}
		});
		
		Dimension fieldSize = new Dimension(200, 30);  // 원하는 너비와 높이 설정
		t_input.setPreferredSize(fieldSize);
		
		Dimension buttonSize = new Dimension(110, 30);  // 원하는 너비와 높이 설정
		b_send.setPreferredSize(buttonSize);
		
		p.add(t_input);
		p.add(b_send);
		
	    t_input.setEnabled(false);
	    b_send.setEnabled(false);
		
		return p;
	}
	
	// 콤보에 따른 아이템을 사용하도록 해주는 UI
	private JPanel createComboPanel() {
		JPanel p = new JPanel(new FlowLayout());
		
		JLabel combo = new JLabel("Combo: ");
		t_combo = new JTextField(3);
		b_combo = new JButton("아이템 사용");
		
		b_combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			items();		// 아이템 사용
			t_combo.setText("");
			}			
		});
		
		p.add(combo);
		p.add(t_combo);
		p.add(b_combo);
		
		t_combo.setEditable(false);
		b_combo.setEnabled(false);
		
		return p;
	}
	
	// 채팅과 숫자 추리를 할 수 있게 하는 UI
	private JPanel createChattingPanel() {
		JPanel p = new JPanel(new BorderLayout());
	    document1 = new DefaultStyledDocument(); // document 변수 초기화
	    
		t_chatting = new JTextPane(document1);
		ChattingScroll = new JScrollPane(t_chatting);  // t_chatting에 JScrollPane 적용
		
		t_chat = new JTextField();
	
		b_guessNum = new JButton("추리");
		b_guessNum.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				guessNumber();
			}
		});
		
		b_talk = new JButton("채팅");
		b_talk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		
		JPanel p_input = new JPanel(new FlowLayout());

		p_input.add(t_chat);
		p_input.add(b_guessNum);
		p_input.add(b_talk);
		
		Dimension fieldSize = new Dimension(180, 30);  // 원하는 너비와 높이 설정
		t_chat.setPreferredSize(fieldSize);
		
		Dimension buttonSize = new Dimension(70, 30);  // 원하는 너비와 높이 설정
		b_guessNum.setPreferredSize(buttonSize);
		b_talk.setPreferredSize(buttonSize);
		
		p.add(ChattingScroll,BorderLayout.CENTER);
		p.add(p_input, BorderLayout.SOUTH);
		
		t_chatting.setEditable(false);
		t_chat.setEditable(false);
		b_guessNum.setEnabled(false);
		b_talk.setEnabled(false);
		
		return p;
	}
	
	// 추리한 숫자와 그에 대한 결과를 저장하는 UI
	private JPanel createDataPanel() {
	    JPanel p = new JPanel(new GridLayout(1, 3));
	    document2 = new DefaultStyledDocument(); // document 변수 초기화
	    document3 = new DefaultStyledDocument(); // document 변수 초기화

	    JPanel leftpanel = new JPanel(new BorderLayout());
	    JLabel numberData = new JLabel("숫자");
	    t_numberData = new JTextPane(document2);
	    DataScroll = new JScrollPane(t_numberData);
	    leftpanel.add(numberData, BorderLayout.NORTH);
	    leftpanel.add(DataScroll, BorderLayout.CENTER);

	    JPanel rightpanel = new JPanel(new BorderLayout());
	    JLabel resultData = new JLabel("결과");
	    t_resultData = new JTextPane(document3);
	    ResultScroll = new JScrollPane(t_resultData);
	    rightpanel.add(resultData, BorderLayout.NORTH);
	    rightpanel.add(ResultScroll, BorderLayout.CENTER);

	    JPanel resultpanel = new JPanel(new BorderLayout());
	    imagePanel = new JPanel();
	    imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.Y_AXIS));
	    imagePanel.setBackground(Color.WHITE);
	    
	    imageScroll = new JScrollPane(imagePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    JLabel imageLabel = new JLabel("이미지", JLabel.CENTER);
	    resultpanel.add(imageLabel, BorderLayout.NORTH);
	    resultpanel.add(imageScroll, BorderLayout.CENTER);
	    
	    
	    p.add(leftpanel, BorderLayout.WEST);
	    p.add(rightpanel, BorderLayout.CENTER);
	    p.add(resultpanel, BorderLayout.EAST);
	    
	    t_numberData.setEditable(false);
	    t_resultData.setEditable(false);
	    

	    return p;
	}
	
	// 닉네임, 주소, 방 번호를 지정하는 UI
	private JPanel createInfoPanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		t_userID = new JTextField(7);
		t_hostAddr = new JTextField(12);
		t_portNum = new JTextField(5);

		t_userID.setText("guest" + getLocalAddr().split("\\.")[3]);
		t_hostAddr.setText(this.serverAddress);
		t_portNum.setText(String.valueOf(this.serverPort));

		t_portNum.setHorizontalAlignment(JTextField.CENTER);

		p.add(new JLabel("닉네임:"));
		p.add(t_userID);

		p.add(new JLabel("접속주소:"));
		p.add(t_hostAddr);

		p.add(new JLabel("방 번호:"));
		p.add(t_portNum);
		
		b_connect = new JButton("접속하기");
		b_connect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NumbaseballClient.this.serverAddress = t_hostAddr.getText();
				NumbaseballClient.this.serverPort = Integer.parseInt(t_portNum.getText());

				try {
					connectToServer();
					sendUserID();
				} catch (Exception e1) {
					printDisplay("서버와의 연결 오류: " + e1.getMessage());
					return;
				}

			}
		});

		b_disconnect = new JButton("전송끊기");
		b_disconnect.setEnabled(false);
		b_disconnect.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
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
		
		return p;
	}
	
	// 서버에 접속하는 함수
	private void connectToServer() throws UnknownHostException, IOException {
		socket = new Socket();
		SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
		socket.connect(sa, 3000);

		out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

		receiveThread = new Thread(new Runnable() {

			private ObjectInputStream in;

			private void receiveMessage() {
				try {
					ChatMsg inMsg = (ChatMsg) in.readObject();
					if (inMsg == null) {
						printDisplay("서버와의 연결이 종료되었습니다.\n");
						disconnect();
						return;
					}
					switch (inMsg.mode) {
					case ChatMsg.MODE_Guess_NUM:
					    printDisplayNumber(String.valueOf(inMsg.guessNumber));
					    printDisplayNumber("\n");

					    printDisplayResult(inMsg.compareResult);
					    printDisplayResult("\n");

					    processResult(inMsg.compareResult);  // 이미지 출력 메소드 호출
					    
					    if (!inMsg.compareResult.contains("S")) {
					        combo++;
					        t_combo.setText(String.valueOf(combo)); // 증가된 combo 값을 t_combo 필드에 반영
					    }
					    break;
					case ChatMsg.MODE_TX_STRING:
						printDisplay(inMsg.userID + ": " + inMsg.message);
						break;
					case ChatMsg.MODE_Compare_Result:
			            processResult(inMsg.compareResult);
			            break;
					case ChatMsg.MODE_3_Hint:
		                printDisplay(inMsg.hintMessage);
		                break;
		            case ChatMsg.MODE_4_Hint:
		                printDisplay(inMsg.hintMessage);
		                break;
		            case ChatMsg.MODE_5_Hint:
		                printDisplay(inMsg.hintMessage);
		                break;
		            case ChatMsg.MODE_6_Hint:
		                printDisplay(inMsg.hintMessage);
		                break;
		            case ChatMsg.MODE_7_Hint:
		                SwingUtilities.invokeLater(new Runnable() {
		                    public void run() {
		                        t_numberData.setText("");
		                        t_resultData.setText("");
		                        t_chatting.setText("");
		                        imagePanel.removeAll();
		                        imagePanel.revalidate();
		                        imagePanel.repaint();
				                printDisplay("기록 지우기 아이템에 당했습니다.. \n");
		                    }
		                });
		                break;
					}
				} catch (IOException e) {
					System.out.println("연결을 종료했습니다.");
				} catch (ClassNotFoundException e) {
					printDisplay("잘못된 객체가 전달되었습니다.");
				}
			}

			@Override
			public void run() {
				try {
					in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				} catch (IOException e) {
					printDisplay("입력 스트림이 열리지 않음");
				}
				while (receiveThread == Thread.currentThread()) {
					receiveMessage();
				}
			}
		});
		receiveThread.start();
		
	    t_input.setEnabled(true);
	    b_send.setEnabled(true);
		b_combo.setEnabled(true);
		b_disconnect.setEnabled(true);
		b_connect.setEnabled(false);
		b_exit.setEnabled(false);
		t_chat.setEditable(true);
		b_guessNum.setEnabled(true);
		b_talk.setEnabled(true);
	}
	
	// 서버와의 연결을 종료하는 함수
	private void disconnect() {
		send(new ChatMsg(uid, ChatMsg.MODE_LOGOUT));
		try {
			receiveThread = null;
			socket.close();
		} catch (IOException e) {
			System.err.println("클라이언트 닫기 오류 -> " + e.getMessage());
			System.exit(-1);
		}
		t_input.setEnabled(true);
		b_send.setEnabled(true);
		
		t_input.setText("");
		t_combo.setText("");
		t_chatting.setText("");
		t_numberData.setText("");
		t_resultData.setText("");
		
        imagePanel.removeAll();
        imagePanel.revalidate();
        imagePanel.repaint();
        
	    t_input.setEnabled(false);
	    b_send.setEnabled(false);
		b_connect.setEnabled(true);
		b_exit.setEnabled(true);
		b_disconnect.setEnabled(false);
		b_combo.setEnabled(false);
		b_guessNum.setEnabled(false);
		b_talk.setEnabled(false);
		t_chat.setEditable(false);
	}
	

	// 보내기 버튼 누를 때 서버에 내 숫자를 전송하는 기능
	private void chooseNumber() {
	    String inputText = t_input.getText();
	    int[] inputNumbers = new int[NUM_MAX_LENGTH];

	    if (inputText.length() != NUM_MAX_LENGTH) {
	        printDisplay("4자리 숫자를 입력하세요.");
	        return;
	    }
	    else if(hasDuplicateDigits(inputText)) {
	    	printDisplay("각 자리의 숫자가 중복되지 않게 입력하세요");
	    	return;
	    }

	    try {
	        for (int i = 0; i < NUM_MAX_LENGTH; i++) {
	            char ch = inputText.charAt(i);
	            if (!Character.isDigit(ch)) {
	                throw new NumberFormatException();
	            }
	            inputNumbers[i] = Character.digit(ch, 10);
	        }
	        send(new ChatMsg(uid, ChatMsg.MODE_Choose_NUM, inputNumbers));
	    } catch (NumberFormatException e) {
	        printDisplay("4자리 숫자를 입력하세요.");
	    }

	    t_input.setEnabled(false);
	    b_send.setEnabled(false);
	}
	
	//숫자를 서버에 전송해 추리하는 함수
	private void guessNumber() {
	    String numberString = t_chat.getText();

	    if (!numberString.isEmpty()) {
	        try {
	            int number = Integer.parseInt(numberString);
	            
	            if (numberString.length() != 4) { // 숫자의 길이가 4가 아니면
	                printDisplay("4자리 숫자를 입력하세요");
	            } else if (hasDuplicateDigits(numberString)) { // 숫자가 중복되면
	                printDisplay("각 자리의 숫자가 중복되지 않게 입력하세요");
	            } else {
	                send(new ChatMsg(uid, ChatMsg.MODE_Guess_NUM, number));
	                t_chat.setText("");
	            }
	            
	        } catch (NumberFormatException e) {
	            printDisplay("4자리 숫자를 입력하세요");
	        }
	    } 
	}

	private boolean hasDuplicateDigits(String numberString) {
	    Set<Character> digitSet = new HashSet<>();
	    for (char digit : numberString.toCharArray()) {
	        if (!digitSet.add(digit)) {
	            return true;
	        }
	    }
	    return false;
	}
	
	//object 전송 기능을 관리하는 함수
	private void send(ChatMsg msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			System.err.println("클라이언트 일반 전송 오류 -> " + e.getMessage());
		}
	}	
	
	//채팅에서 상대방과 채팅을 주고 받는 함수
	private void sendMessage() {
		String message = t_chat.getText();
		if (message.isEmpty())
			return;

		send(new ChatMsg(uid, ChatMsg.MODE_TX_STRING, message));

		t_chat.setText("");
	}
	
	
	//사용자의 닉네임을 전송하는 함수
	private void sendUserID() {
		uid = t_userID.getText();
		send(new ChatMsg(uid, ChatMsg.MODE_LOGIN));
	}
	
	// 콤보에 따라 사용할 아이템에 대한 함수
	private void items() {
		if(combo == 3 ) {
			combo3Items();
			combo -= 3;
		} else if(combo == 4) {
			combo4Items();
			combo -= 4;
		} else if(combo == 5) {
			combo5Items();
			combo -= 5;
		} else if(combo == 6) {
			combo6Items();
			combo -= 6;
		} else if(combo >= 7) {
			combo7Items();
			combo -= 7;
		} else {return;}
	}
		
	private void combo3Items() {
		send(new ChatMsg(uid, ChatMsg.MODE_3_Hint));
		// 상대방 1번째 숫자 알기
	}
	
	private void combo4Items() {
		send(new ChatMsg(uid, ChatMsg.MODE_4_Hint));
		// 상대방 2번째 숫자 알기
	}
	
	private void combo5Items() {
		send(new ChatMsg(uid, ChatMsg.MODE_5_Hint));
		// 상대방 3번째 숫자 알기
	}
	
	private void combo6Items() {
		send(new ChatMsg(uid, ChatMsg.MODE_6_Hint));
		// 상대방 4번째 숫자 알기
	}
	
	private void combo7Items() {
		send(new ChatMsg(uid, ChatMsg.MODE_7_Hint));
		// 상대방 저장 기록 날리기
	}
 	
	
	// 채팅창에 글자를 표시하는 함수
	private void printDisplay(String msg) {
		int len = t_chatting.getDocument().getLength();  
		try {
			document1.insertString(len,  msg + "\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		 
		t_chatting.setCaretPosition(len);  
	  }
	
	// 추리한 숫자들 기록해두는 기능 
	private void printDisplayNumber(String msg) {
		int len = t_numberData.getDocument().getLength();  
		try {
			document2.insertString(len,  msg + "\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		 
		t_numberData.setCaretPosition(len);
	  }
	
	// 추리한 숫자들에 대한 결과를 기록하는 기능
	private void printDisplayResult(String msg) {
		int len = t_resultData.getDocument().getLength();  
		try {
			document3.insertString(len,  msg + "\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		 
		t_resultData.setCaretPosition(len);  
	  }
	
	// 서버로부터 받은 compareNumbers의 결과를 기반으로 이미지를 출력
	private void processResult(String result) {
	    displayImages(result, 90, 40);
	}
	
	// 이미지를 화면에 출력하는 메서드
	private void displayImage(String imageName, int width, int height) {
		try {
			// 이미지 로딩
	        InputStream resourceStream = getClass().getResourceAsStream(imageName);
	        if (resourceStream != null) {
	            // 이미지 아이콘 생성
	            ImageIcon icon = new ImageIcon(ImageIO.read(resourceStream));

	            // 이미지 크기 조절
	            Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
	            icon = new ImageIcon(scaledImage);

	            // JLabel을 생성하여 이미지를 표시
	            JLabel label = new JLabel(icon);
	            
	            // 이미지를 이미지 패널에 추가
                imagePanel.add(label);
                imagePanel.add(Box.createRigidArea(new Dimension(0, 7)));
                imagePanel.revalidate();
                imagePanel.repaint();   
		} else {
            printDisplay("이미지 로딩 실패: " + imageName);
        } 
	        	
	} catch (Exception e) {
		e.printStackTrace();
		printDisplay("이미지 로딩 오류: " + e.getMessage());
	}
}

	// compareNumbers의 결과에 따라 이미지 파일 이름을 결정하여 이미지 출력
	private void displayImages(String compareResult, int width, int height) {
		// 이미지 파일 이름을 결정
	    String imageName = determineImageName(compareResult);
	    // 이미지 출력
	    displayImage(imageName, width, height);
	}
	
	private String determineImageName(String compareResult) {
	    // compareResult 값에 따라 이미지 파일 이름 결정 로직을 구현
	    if (compareResult.equals("아웃!")) {
	        return "1.jpg";
	    } else if (compareResult.equals("1B ")) {
	        return "2.jpg";
	    } else if (compareResult.equals("2B ")) {
	        return "3.jpg";
	    } else if (compareResult.equals("3B ")) {
	        return "4.jpg";
	    } else if (compareResult.equals("4B ")) {
	        return "5.jpg";
	    } else if (compareResult.equals("1S ")) {
	        return "6.jpg";
	    } else if (compareResult.equals("1S 1B ")) {
	        return "7.jpg";
	    } else if (compareResult.equals("1S 2B ")) {
	        return "8.jpg";
	    } else if (compareResult.equals("1S 3B ")) {
	        return "9.jpg";
	    } else if (compareResult.equals("2S ")) {
	        return "10.jpg";
	    } else if (compareResult.equals("2S 1B ")) {
	        return "11.jpg";
	    } else if (compareResult.equals("2S 2B ")) {
	        return "12.jpg";
	    } else if (compareResult.equals("3S ")) {
	        return "13.jpg";
	    }
	    else {
	        return "14.jpg";
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
		String serverAddress = "localhost";
		int serverPort = 54321;

		new NumbaseballClient(serverAddress, serverPort);

	}

}