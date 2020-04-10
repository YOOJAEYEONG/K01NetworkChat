package chat2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {

		//.
		System.out.println("이름을 입력하세요");
		Scanner scanner = new Scanner(System.in);
		String s_name = scanner.nextLine();
		
		PrintWriter out = null;
		BufferedReader in = null;
		
		
		try {
			//별도의 매개변수가 없으면 접속IP는  localhost로 고정됨
			//localhost대신 내 아이피로 접속해도 연결된다.
			String ServerIP = "localhost";
			
			//클라이언트 실행시 매개변수가 있는경우 아이피로 설정함
			if(args.length > 0) {
				ServerIP = args[0];
			}
			
			//IP주소와 포트를 기반으로 소켓객체를 생성하여 서버에 접속함
			Socket socket = new Socket(ServerIP, 9999);
			//서버와 연결되면 콘솔에 메시지 출력
			System.out.println("서버와 연결되었습니다.");
			
			/*
			InputStreamReader / OutputStreamReader
			바이트트스림과 문자스트림의 상호변환을 제공하는 입출력 스트림이다.
			바이트를 읽어서 지정된 문자 인코딩에 따라 문자로 변환하는데 사용한다.
			 */
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			
			//접속자의 "대화명"을 서버측으로 최초 전송한다.
			
			out.println(s_name);
			
			
			while(out!=null) {
				try {
					//서버가 echo해준 내용을 라인단위로 읽어와서 콘솔 출력
					if(in!=null) {
						System.out.println("Receive: "+ in.readLine());
					}
					//클라이언트는 내용을 입력 후 서버로 전솓ㅇ한다.
					String s2 = scanner.nextLine();
					
					//입력값이 q면 while 루프탈출
					if(s2.equals("q")|| s2.equals("Q")) {
						break;
					}
					else {
						//아니면 서버로 입력내용전송
						out.println(s2);
					}
					
				} catch (Exception e) {
					System.out.println("예외: " + e);
				}
			}
			
			
			//스트림과 소켓을 종료한다.
			in.close();
			out.close();
			socket.close();
			
		} catch (Exception e) {
			System.out.println("예외발생[MultiClient]"+e);
		}
	}
}



























