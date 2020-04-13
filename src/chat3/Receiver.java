package chat3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class Receiver extends Thread {

	Socket socket;
	BufferedReader in = null;
	
	//소켓객체를 매개변수로 받는 생성자
	public Receiver(Socket socket) {
		this.socket = socket;
		
		//소켓객체를 기반으로 input스트림을 생성한다.
		//서버가 보내는 메세지를 읽어오는 역할이다
		try {
			in = new BufferedReader(
					new InputStreamReader(
							this.socket.getInputStream()));
		} catch (Exception e) {
			System.out.println("예외1: "+ e);
		}
	}
	/*
	Thread에서 main()역할을 하는 함수로 직접호출하면 안되고
	반드시 start()를 통해 간섭호출해야 쓰레드가 생성됨.
	프로그램 시작시 main()호출이 없음에도 자동으로 main()가 실행됨을 참고
	 */
	@Override
	public void run() {
		//스트림을 통해 서버가 보낸 내용을 라인단위로 읽어온다.
		while (in != null) {
			try {
				System.out.println(
						"Thread Receive : "+ in.readLine());
			} catch (Exception e) {
				/*
				클라이언트가 접속을 종료할경우 SocketException이 발생하면서
				무한 루프 발생
				 */
				System.out.println("예외2 : "+ e);
			}
		}
		try {
			in.close();
		} catch (Exception e) {
			System.out.println("예외3:"+e);
		}
	}
}
