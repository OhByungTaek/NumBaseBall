package numbaseball;
import java.io.Serializable;

public class ChatMsg implements Serializable {
	
	public final static int MODE_LOGIN = 0x1;
	public final static int MODE_LOGOUT = 0x2;
	public final static int MODE_Guess_NUM = 0X5;
	public final static int MODE_Choose_NUM = 0X7;
	public final static int MODE_TX_STRING = 0x10;
	public final static int MODE_Compare_Result = 0X15;
	public final static int MODE_3_Hint = 0X16;
	public final static int MODE_4_Hint = 0X17;
	public final static int MODE_5_Hint = 0X18;
	public final static int MODE_6_Hint = 0X19;
	public final static int MODE_7_Hint = 0X20;
	protected static final int MODE_RESULT = 0x30;
	
	String userID;
	int mode;
	int[] chooseNumber;
	int guessNumber;
	public String message;
	public String compareResult;
	public String hintMessage;
	protected String result;
	
	public ChatMsg(String userID, int code, int guessNumber, int[] chooseNumber, String message) {
		this.userID = userID;
		this.mode = code;
		this.guessNumber = guessNumber;
		this.chooseNumber = chooseNumber;
		this.message = message;
	}
	
	
	public ChatMsg(String userID, int code) {
		this(userID, code, 0, null, null);
	}
	
	public ChatMsg(String userID, int code, int guessNumber) {
		this(userID, code, guessNumber, null, null);
	}
	
	public ChatMsg(String userID, int code, int[] chooseNumber) {
		this(userID, code, 0, chooseNumber, null);
	}
	
	public ChatMsg(String userID, int code, String Message) {
		this(userID, code, 0, null, Message);
	}

	
}