package net.hinet.sms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * @author 中華電信數據通信分公司
 */
public class ShortMessageService {

	private Socket socket;

	private DataInputStream dataInputStream;

	private DataOutputStream dataOutputStream;

	private String message = "";

	private String mobileStationInternationalSubscriberDirectoryNumber = "";

	/**
	 * 十六進位轉十進位
	 *
	 * @param input
	 * @return
	 */
	public int HexToDec(String input) {
		int sum = 0;
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) >= '0' && input.charAt(i) <= '9') {
				sum = sum * 16 + input.charAt(i) - 48;
			} else if (input.charAt(i) >= 'A' && input.charAt(i) <= 'F') {
				sum = sum * 16 + input.charAt(i) - 55;
			}
		}
		return sum;
	}

	/**
	 * 建立 socket 連線，並做帳號密碼檢查。
	 *
	 * @param host 伺服器
	 * @param port 通訊埠
	 * @param userAccount 帳號
	 * @param userPassword 密碼
	 * @return -1：網路連線失敗；0：連線與認證成功；1:連線成功，認證失敗。
	 */
	@SuppressWarnings("UnusedAssignment")
	public int connect(String host, int port, String userAccount, String userPassword) {
		//---設定送出訊息訊息的buffer
		byte out_buffer[] = new byte[266]; //傳送長度為266

		//---設定接收訊息的buffer
		byte returnCode = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			//---建 socket
			this.socket = new Socket(host, port);

			this.dataInputStream = new DataInputStream(this.socket.getInputStream());
			this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());

			//---開始帳號密碼檢查
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				out_buffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定帳號與密碼
			String acc_pwd_str = userAccount.trim() + "\0" + userPassword.trim() + "\0";
			byte acc_pwd_byte[] = acc_pwd_str.getBytes();
			byte acc_pwd_size = (byte) acc_pwd_byte.length;

			out_buffer[0] = 0; //檢查密碼
			out_buffer[1] = 1; //big編碼
			out_buffer[2] = 0; //priority
			out_buffer[3] = 0; //國碼 0:台灣
			out_buffer[4] = acc_pwd_size; //msg_set_len
			out_buffer[5] = 0; //msg_content_len, 驗證密碼時不需msg_content
			//設定msg_set 內容 "帳號"+"密碼"
			for (i = 0; i < acc_pwd_size; i++) {
				out_buffer[i + 6] = acc_pwd_byte[i];
			}

			//----送出訊息
			//this.dataOutputStream.write(outputBuffer , 0 , acc_pwd_size + 3 );
			this.dataOutputStream.write(out_buffer);

			//---讀 return code
			returnCode = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);

			return returnCode;
		} catch (UnknownHostException e) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = "Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 結束 socket 連線
	 */
	public void close() {
		try {
			if (this.dataInputStream != null) {
				this.dataInputStream.close();
			}
			if (this.dataOutputStream != null) {
				this.dataOutputStream.close();
			}
			if (this.socket != null) {
				this.socket.close();
			}

			this.dataInputStream = null;
			this.dataOutputStream = null;
			this.socket = null;
		} catch (UnknownHostException e) {
			this.message = "Cannot find the host!";
		} catch (IOException ex) {
			this.message = "Socket Error: " + ex.getMessage();
		}
	}

	/**
	 * 即時傳送文字簡訊
	 *
	 * @param sms_tel
	 * @param message
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int sendTextMessage(String sms_tel, String message) {
		/*
		 設定送出訊息訊息的 buffer
		 */
		byte out_buffer[] = new byte[266];//傳送長度為266

		/*
		 設定接收的 buffer
		 */
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				out_buffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定傳送訊息的內容 01:即時傳送
			String msg_set = sms_tel.trim() + "\0" + "01" + "\0";
			byte msg_set_byte[] = msg_set.getBytes();
			int msg_set_size = msg_set_byte.length;

			String msg_content = message.trim() + "\0";
			byte msg_content_byte[] = msg_content.getBytes("Big5"); //需指定轉碼為Big5，不然會印出??
			int msg_content_size = msg_content_byte.length - 1; //send_type=1時,長度不包含'\0'

			if (msg_set_size > 80) {
				this.message = "msg_set > max limit!";
				return 80;
			}
			if (msg_content_size > 159) {
				this.message = "msg_content > max limit!";
				return 81;
			}

			//---設定送出訊息的 buffer
			if (sms_tel.startsWith("+")) {
				out_buffer[0] = 15; //send text 國際簡訊
			} else {
				out_buffer[0] = 1; //send text 國內簡訊
			}
			out_buffer[1] = 1; //big5編碼
			out_buffer[2] = 0; //priority
			out_buffer[3] = 0; //國碼 0:台灣
			out_buffer[4] = (byte) msg_set_size; //msg_set_len
			out_buffer[5] = (byte) msg_content_size; //msg_content_len

			//設定msg_set 內容 "手機號碼"+"傳送形式"
			for (i = 0; i < msg_set_size; i++) {
				out_buffer[i + 6] = msg_set_byte[i];
			}

			//設定msg_content 內容 "訊息內容"
			for (i = 0; i < msg_content_size; i++) {
				out_buffer[i + 106] = msg_content_byte[i];
			}

			//----送出訊息
			this.dataOutputStream.write(out_buffer);

			//---讀 return code
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);
			this.message = this.message.trim();

			return ret_code;
		} catch (UnknownHostException eu) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = " Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 預約傳送文字簡訊
	 *
	 * @param cellularNumber
	 * @param message
	 * @param order_time
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int sendTextMessage(String cellularNumber, String message, String order_time) {
		/*
		 設定送出訊息訊息的 buffer
		 */
		byte[] outputBuffer = new byte[266];//傳送長度為266

		/*
		 設定接收的 buffer
		 */
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			for (int i = 0; i < 266; i++) {
				outputBuffer[i] = 0;//清除 buffer
			}
			for (int i = 0; i < ret_set.length; i++) {
				ret_set[i] = 0;
			}
			for (int i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定傳送訊息的內容 03:預約傳送
			String msg_set = cellularNumber.trim() + "\0" + "03" + "\0" + order_time.trim();
			byte msg_set_byte[] = msg_set.getBytes();
			int msg_set_size = msg_set_byte.length;

			String msg_content = message.trim() + "\0";
			byte msg_content_byte[] = msg_content.getBytes("Big5"); //需指定轉碼為Big5，不然會印出??
			int msg_content_size = msg_content_byte.length - 1; //send_type=1時,長度不包含'\0'

			if (msg_set_size > 80) {
				this.message = "msg_set > max limit!";
				return 80;
			}
			if (msg_content_size > 159) {
				this.message = "msg_content > max limit!";
				return 81;
			}

			//---設定送出訊息的 buffer
			if (cellularNumber.startsWith("+")) {
				outputBuffer[0] = 15; //send text 國際簡訊
			} else {
				outputBuffer[0] = 1; //send text 國內簡訊
			}
			outputBuffer[1] = 1; //big5編碼
			outputBuffer[2] = 0; //priority
			outputBuffer[3] = 0; //國碼 0:台灣
			outputBuffer[4] = (byte) msg_set_size; //msg_set_len
			outputBuffer[5] = (byte) msg_content_size; //msg_content_len

			//設定msg_set 內容 "手機號碼"+"傳送形式"+"預約時間"
			for (int i = 0; i < msg_set_size; i++) {
				outputBuffer[i + 6] = msg_set_byte[i];
			}

			//設定msg_content 內容 "訊息內容"
			for (int i = 0; i < msg_content_size; i++) {
				outputBuffer[i + 106] = msg_content_byte[i];
			}

			//----送出訊息
			this.dataOutputStream.write(outputBuffer);

			//---讀 return code
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);
			this.message = this.message.trim();

			return ret_code;
		} catch (UnknownHostException eu) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = " Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 查詢文字簡訊的傳送結果
	 *
	 * @param type 2: text, 6: logo, 8: ringtone, 10: picmsg, 14: wappush
	 * @param messageid
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int query_message(int type, String messageid) {
		//---設定送出訊息的buffer
		byte out_buffer[] = new byte[266]; //傳送長度為266

		//----設定接收的buffer
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				out_buffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定message id
			String msg_set = messageid.trim() + "\0";
			byte msg_set_byte[] = msg_set.getBytes();
			int msg_set_size = msg_set_byte.length;

			if (msg_set_size > 80) {
				this.message = "msg_set > max limit!";
				return 80;
			}

			//---設定送出訊息的 buffer
			out_buffer[0] = (byte) type; //query type  02:text ,06:logo, 08 ringtone, 10:picmsg, 14:wappush
			out_buffer[1] = 1; //big5編碼
			out_buffer[2] = 0; //priority
			out_buffer[3] = 0; //國碼 0:台灣
			out_buffer[4] = (byte) msg_set_size; //msg_set_len
			out_buffer[5] = 0;  //msg_content_len

			//設定messageid
			for (i = 0; i < msg_set_size; i++) {
				out_buffer[i + 6] = msg_set_byte[i];
			}

			//----送出訊息
			this.dataOutputStream.write(out_buffer);

			//---讀 return code
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);
			this.message = this.message.trim();

			return ret_code;
		} catch (UnknownHostException eu) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = " Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 接收文字簡訊
	 *
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int recv_text_message() {
		//---設定送出訊息訊息的buffer
		byte out_buffer[] = new byte[266];//傳送長度為266

		//----設定接收的buffer
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				out_buffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定送出訊息的 buffer
			out_buffer[0] = 3; //recv text message
			out_buffer[1] = 1; //big5編碼
			out_buffer[2] = 0; //priority
			out_buffer[3] = 0; //國碼 0:台灣
			out_buffer[4] = 0; //msg_set_len
			out_buffer[5] = 0; //msg_content_len

			//----送出訊息
			this.dataOutputStream.write(out_buffer);

			//---讀 return code
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content, "big5");
			this.message = this.message.trim();

			this.mobileStationInternationalSubscriberDirectoryNumber = "";
			//ret_code==0 表示有資料，則取出傳送端的手機號碼
			if (ret_code == 0) {
				String ret_set_msg = new String(ret_set);
				//將string用'\0'分開，
				StringTokenizer stringTokenizer = new StringTokenizer(ret_set_msg, "\0");
				if (stringTokenizer.hasMoreTokens()) {
					this.mobileStationInternationalSubscriberDirectoryNumber = stringTokenizer.nextToken();
				}
			}

			return ret_code;
		} catch (UnknownHostException eu) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = " Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 取消預約文字簡訊
	 *
	 * @param messageId
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int cancel_text_message(String messageId) {
		//---設定送出訊息的buffer
		byte out_buffer[] = new byte[266]; //傳送長度為266

		//----設定接收的buffer
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				out_buffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			//---設定message id
			String msg_set = messageId.trim() + "\0";
			byte msg_set_byte[] = msg_set.getBytes();
			int msg_set_size = msg_set_byte.length;

			if (msg_set_size > 80) {
				this.message = "msg_set > max limit!";
				return 80;
			}

			//---設定送出訊息的 buffer
			out_buffer[0] = 16; //取消預約簡訊
			out_buffer[1] = 1; //big5編碼
			out_buffer[2] = 0; //priority
			out_buffer[3] = 0; //國碼 0:台灣
			out_buffer[4] = (byte) msg_set_size; //msg_set_len
			out_buffer[5] = 0;  //msg_content_len

			//設定messageid
			for (i = 0; i < msg_set_size; i++) {
				out_buffer[i + 6] = msg_set_byte[i];
			}

			//----送出訊息
			this.dataOutputStream.write(out_buffer);

			//---讀 return code
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			//---讀 return message
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);
			this.message = this.message.trim();

			return ret_code;
		} catch (UnknownHostException eu) {
			this.message = "Cannot find the host!";
			return 70;
		} catch (IOException ex) {
			this.message = " Socket Error: " + ex.getMessage();
			return 71;
		}
	}

	/**
	 * 傳送 WapPush 簡訊
	 *
	 * @param mobileNumber 接收門號
	 * @param sms_url
	 * @param message 簡訊內容
	 * @return
	 */
	@SuppressWarnings("UnusedAssignment")
	public int send_wappush_message(String mobileNumber, String sms_url, String message) {
		/*
		 設定送出訊息訊息的buffer
		 */
		byte[] outputBuffer = new byte[266];//傳送長度為266

		/*
		 設定接收的buffer
		 */
		byte ret_code = 99;
		byte ret_coding = 0;
		byte ret_set_len = 0;
		byte ret_content_len = 0;
		byte ret_set[] = new byte[80];
		byte ret_content[] = new byte[160];

		try {
			int i;
			//----清除 buffer
			for (i = 0; i < 266; i++) {
				outputBuffer[i] = 0;
			}
			for (i = 0; i < 80; i++) {
				ret_set[i] = 0;
			}
			for (i = 0; i < 160; i++) {
				ret_content[i] = 0;
			}

			/*
			 設定傳送訊息的內容 01:SI
			 */
			String msg_set = mobileNumber.trim() + "\0" + "01" + "\0";
			byte msg_set_byte[] = msg_set.getBytes();
			int msg_set_size = msg_set_byte.length;

			String msg_content = sms_url.trim() + "\0" + message.trim() + "\0";
			byte msg_content_byte[] = msg_content.getBytes("Big5"); //需指定轉碼為Big5，不然會印出??
			int msg_content_size = msg_content_byte.length;

			/*
			 設定送出訊息的 buffer
			 */
			outputBuffer[0] = 13; //send wappush
			outputBuffer[1] = 1; //big編碼
			outputBuffer[2] = 0; //priority
			outputBuffer[3] = 0; //國碼 0:台灣
			outputBuffer[4] = (byte) msg_set_size; //msg_set_len
			outputBuffer[5] = (byte) msg_content_size; //msg_content_len

			/*
			 設定msg_set 內容="手機號碼"+"傳送形式"
			 */
			for (i = 0; i < msg_set_size; i++) {
				outputBuffer[i + 6] = msg_set_byte[i];
			}

			/*
			 設定msg_content 內容="url"+"訊息內容"
			 */
			for (i = 0; i < msg_content_size; i++) {
				outputBuffer[i + 106] = msg_content_byte[i];
			}

			this.dataOutputStream.write(outputBuffer);//送出訊息

			/*
			 讀 return code
			 */
			ret_code = this.dataInputStream.readByte();
			ret_coding = this.dataInputStream.readByte();
			ret_set_len = this.dataInputStream.readByte();
			ret_content_len = this.dataInputStream.readByte();

			/*
			 讀 return message
			 */
			this.dataInputStream.read(ret_set, 0, 80);
			this.dataInputStream.read(ret_content, 0, 160);
			this.message = new String(ret_content);
			this.message = this.message.trim();
			return ret_code;
		} catch (UnknownHostException eu) {
			System.out.println(" Cannot find the host ");
			return 70;
		} catch (IOException ex) {
			System.out.println(" Socket Error: " + ex.getMessage());
			return 71;
		}
	}

	public String getMessage() {
		return this.message;
	}

	public String get_msisdn() {
		return mobileStationInternationalSubscriberDirectoryNumber;
	}

	/**
	 * 主函式 - 使用文字簡訊範例
	 *
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("UseSpecificCatch")
	private static void main(String[] args) throws Exception {
		try {
			String server = "202.39.54.130";//hiAirV2 Gateway IP
			int port = 8000;//Socket to Air Gateway Port

			if (args.length < 4) {
				System.out.println("Use: java sms2 id passwd tel message");
				System.out.println(" Ex: java sms2 test test123 0910123xxx HiNet簡訊!");
				return;
			}

			String user = args[0];//帳號
			String passwd = args[1];//密碼
			String tel = args[2];//手機號碼
			String message = new String(args[3].getBytes(), "big5");//簡訊內容

			/*
			建立連線 and 檢查帳號密碼是否錯誤
			 */
			ShortMessageService shortMessageService = new ShortMessageService();
			int k = shortMessageService.connect(server, port, user, passwd);
			if (k == 0) {
				System.out.println("帳號密碼check ok!");
			} else {
				System.out.println(shortMessageService.getMessage());

				shortMessageService.close();//結束連線
				return;
			}

			k = shortMessageService.sendTextMessage(tel, message);
			if (k == 0) {
				System.out.println("簡訊已送到簡訊中心!");
				System.out.println("MessageID=" + shortMessageService.getMessage());
			} else {
				System.out.println("簡訊傳送發生錯誤!");
				System.out.print("ret_code=" + k + ",");
				System.out.println("ret_content=" + shortMessageService.getMessage());

				shortMessageService.close();//結束連線
				return;
			}

			shortMessageService.close();//結束連線
		} catch (Exception e) {
			System.out.println("I/O Exception : " + e);
		}
	}
}
