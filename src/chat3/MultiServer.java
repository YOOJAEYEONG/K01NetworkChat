package chat3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	static PrintWriter out = null;
	static BufferedReader in = null;
	static String s = "";
	
	
	public MultiServer() {
		//실행부없음
	}
	
	//서버의 초기화를 담당할 메소드
	public static void init() {
		//클라이언트로부터 전송받은 이름을 저장
		String name = "";
		
		try {
			
			//9999포트를 열고 클라이언트의 접속을 대기
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			//클라이언트의 접속요청을 허가한다.
			socket = serverSocket.accept();
			System.out.println(socket.getInetAddress()+
					":"+socket.getPort());
			
			//클라이언트로 메세지를 보낼 준비(output stream)
			out = new PrintWriter(socket.getOutputStream(), true);
			//클라이언트가 보낸 메시지를 읽을 준비(input stream)
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			//최초로 입력하게 되는 값은 이름을 입력하도록 되어있다.
			if(in != null) {
				name = in.readLine();
				//이름을 콘솔에 출력하고
				System.out.println(name+"접속");
				//클라이언트로 Echo 한다.
				out.println(">"+name+"님이 접속했습니다.");
			}
			//클라이언트가 전송하는 메세지를 계속 읽어온다.
			while (in != null) {
				s = in.readLine();
				if(s == null) {
					break;
				}
				//나중에 대화목록을 저장할때 DB처리는 여기서 하면된다.
				System.out.println(name+"==>"+s);
				//클라이언트에게 Echo해준다.
				sendAllMsg(name, s);
			}
			System.out.println("bye~~~~");
		} catch (Exception e) {
			System.out.println("예외1"+e);
		}
		finally {
			try {
				in.close();
				out.close();
				socket.close();
				serverSocket.close();
			} catch (Exception e) {
				System.out.println("예외2:"+e);
			}
		}
	}
	//서버가 클라이언트에게 메세지를 echo해주는 메소드
	public static void sendAllMsg(String name, String msg) {
		try {
			out.println(">"+name+"==> "+ msg);
		} catch (Exception e) {
			System.out.println("예외3: "+ e);
		}
	}
	
	public static void main(String[] args) {
		init();
	}
}



























