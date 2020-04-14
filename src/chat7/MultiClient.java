package chat7;

import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {

		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		String ServerIP = "localhost";
		
		if(args.length > 0) {ServerIP = args[0];}
		
		
		System.out.println("이름을 입력하세요");
		String s_name = scanner.nextLine();
		
//		try {
//			
//		} catch (UnsupportedEncodingException e) {}
		try {
			
			Socket socket = new Socket(ServerIP, 9999);
			System.out.println("서버와 연결되었습니다.");

			//서버에서 보내는 Echo메세지를 클라이언트에 출력하기 위한 쓰레드 생성
			Thread receiver = new Receiver(socket);

			//프로그램의 종료와 상관없이 계속 실행되는 독립쓰레드
			receiver.start();

			//클라이언트의 메세지를 서버로 전송해주는 쓰레드 생성
			Thread sender = new Sender(socket, s_name);
			sender.start();
			
		} catch (Exception e) {
			System.out.println("예외발생[MultiClient]:" + e);
		}
	}
}



























