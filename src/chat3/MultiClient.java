package chat3;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {

		System.out.println("이름을 입력하세요");
		Scanner scanner = new Scanner(System.in);
		String s_name = scanner.nextLine();
		
		PrintWriter out = null;
		//서버의 메세지를 읽어오는 기능이 Receiver로 옮겨짐
		//BufferedReader in = null;
		
		try {
			/*
			c:\> java 패키지명.Multiclient [접속할IP주소]
				=> 해당 IP주소로 접속할 수 있다.
				만약 IP주소가 없다면 localhost(127.0.0.1)로 접속된다.
			 */
			String ServerIP = "localhost";
			if(args.length > 0) {
				ServerIP = args[0];
			}
			
			Socket socket = new Socket(ServerIP, 9999);
			System.out.println("서버와 연결되었습니다.");
			
			//서버에서 보내는 메세지를 읽어올 Receiver쓰레드 시작
			Thread receiver = new Receiver(socket);
			//setDaemon(true); 가 없으므로 독립쓰레드로 생성됨
			//프로그램의 종료와 상관없이 계속 실행되는 독립쓰레드
			receiver.start();
			
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println(s_name);
			
			while (out != null) {

				try {
					String s2 = scanner.nextLine();
					
					if(s2.equalsIgnoreCase("q")) {
						break;
					}
					else {
						//클라이언트의 메세지를 서버로 전송한다.
						out.println(s2);
					}
				} catch (Exception e) {
					System.out.println("예외:" + e);
				}
			}
			out.close();
			socket.close();
			scanner.close();
		} catch (Exception e) {
			System.out.println("예외발생[MultiClient]:" + e);
		}
	}
}



























