package chat2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {

	public static void main(String[] args) {

		ServerSocket serverSocket = null;//서버를 개발할때 사용
		//ServerSocket(int port) : 지정된 포트번호에 바인딩된 서버소켓을 만든다.
		//ServerSocket(int port, int blocking) : 대기중인 최대 연결수를 추가지정한다.
		
		
		Socket socket = null;//서버로 접속하거나 accept하는데 사용
		PrintWriter out = null;
		BufferedReader in = null;
		String s = "";//클라이언트의 메시지를 저장
		String name = "";//클라이언트의 이름을 저장
		
		try {
			//포트번호 9999번으로 설정하여 서버를 생성하고 클라이언트의 접속을 기다린다.
			serverSocket = new ServerSocket(9999);
			System.out.println("서버시작됨");
			//.....접속대기중.....
			
			//클라이언트가 접속 요청을 하면  accept()를 통해 받아들인다.
			//객체가 반환이 되면(연결이 되면) InputStream을통해 받은 데이터를 읽을수 있다.
			socket = serverSocket.accept();
			
			
			
			//서버->클라이언트 에게 메시지를 전송(출력)하기위한 스트림 생성
			out = new PrintWriter(socket.getOutputStream(), true);
			
			
			//클라이언트로 부터 메시지를 받기 위한 스트림 생성
			//InputStream 은 데이터를 byte로 읽기 때문에 상위 레벨의 데이터로 읽으려면
			//InputStreamReader 로 읽어준다.
			InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
			
			//BufferedReader에 InputStream을 래핑하여 데이터를 String으로 읽어온다.
			in = new BufferedReader(inputStream);
			
			/*
			클라이언트가 서버로 전송하는 최초의 메시지는 "대화명"이므로
			메세지를 읽은 후 변수에 저장하고 클라이언트 쪽으로 Echo해준다.
			 */
			if(in != null) {
				name = in.readLine();
				System.out.println(name +" 접속");
				out.println(">"+name+"님이 접속했습니다.");
			}
			
			/*
			두번째 메시지부터는 실제 대화내용이므로 읽어와서 로그로 출력하고
			동시에 클라이언트로 Echo한다.
			 */
			while (in != null) {
				//클라이언트가 보낸 메시지를 라인단위로 읽어옴
				s = in.readLine();
				if(s == null) {
					break;
				}
				System.out.println(name+" ==> "+s);
				out.println(">"+ name + "==>"+ s);
				//PrintWriter out 의 객채변수임
			}
			System.out.println("byeeeeee");
			
		} catch (Exception e) {
			System.out.println("예외1:"+e);
		}
		finally {
			try {
				//입출력스트림종료
				in.close();
				out.close();
				//소켓종료
				socket.close();
				serverSocket.close();
			} catch (Exception e) {
				System.out.println("예외2:"+e);
			}
		}
	}
}



























