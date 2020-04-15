package chat7;

import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Scanner;


//클라이언트가 입력한 메세지를 서버로 전송해주는 쓰레드 클래스
public class Sender extends Thread{

	Socket socket;
	PrintWriter out = null;
	String name;
	
	
	
	//생성자에서 output스트림을 생성한다.
	public Sender(Socket socket, String name) {
		this.socket = socket;
		try {
			out = new PrintWriter(
					this.socket.getOutputStream(), true);
			this.name = name;
		} catch (Exception e) {
			System.out.println("예외>Sender>생성자"+e);
		}
	}
	
	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		String msg = null;
		
		try {
			//클라이언트가 입력한 "대화명"을 서버로 전송한다.
			out.println(URLEncoder.encode(name, "UTF-8"));
			
			//Q를 입력하기전가지의 메세지를 서버로 전송한다.
			while (out != null) {
				try {
					
					
						msg = scan.nextLine();
						msg = URLEncoder.encode(msg, "UTF-8");
					if(msg.equalsIgnoreCase("Q")) {
						break;
					}
					else {
						out.println(msg);
					}
				} catch (Exception e) {
					System.out.println("예외>Sender>run1: "+ e);
				}
			}
			
			//q를 입력하면 스트림과 소켓을 모두 종료한다.
			out.close();
			socket.close();
			scan.close();
		} catch (Exception e) {
			System.out.println("예외>Sender>run2: "+ e);
		}
	}
}
