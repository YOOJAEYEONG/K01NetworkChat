package chat6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

public class Receiver extends Thread {

	Socket socket;
	BufferedReader in = null;
	
	//소켓객체를 매개변수로 받는 생성자
	public Receiver(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(
					new InputStreamReader(
							this.socket.getInputStream()));
		} catch (Exception e) {
			System.out.println("예외>Receiver>생성자: "+ e);
		}
	}
	/*
	Thread에서 main()역할을 하는 함수로 직접호출하면 안되고
	반드시 start()를 통해 간섭호출해야 쓰레드가 생성됨.
	프로그램 시작시 main()호출이 없음에도 자동으로 main()가 실행됨을 참고
	 */
	@Override
	public void run() {
		//소켓이 종료되면 while()을 벗어나서 input스트림을 종료한다.
		while (in != null) {
			try {
				System.out.println(
						"Thread Receive : "+ in.readLine());
			} catch (SocketException e) {
				System.out.println("SocketException 발생됨");
				break;
			} catch (Exception e) {
				System.out.println("예외>Receiver>run1 : "+ e);
			}
		}
		try {
			in.close();
		} catch (Exception e) {
			System.out.println("예외>Receiver>run2:"+e);
		}
	}
}
