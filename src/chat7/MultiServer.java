package chat7;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	DBHandler dbHandler;
	
	//클라이언트 정보 저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap;
	
	
	public MultiServer() {
		//클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		clientMap = new HashMap<String, PrintWriter>();
		//HashMap동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는것을 차단한다.
		Collections.synchronizedMap(clientMap);
		
		
	}
	
	//서버 초기화
	public void init() {
		try {
			serverSocket = new ServerSocket(9999);
			
			//서버시작후 DB테이블 생성 및 연동 준비
			dbHandler = new DBHandler();
			
			/*
			클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 
			쓰레드 생성및 start.
			*/
			while (true) {
				socket = serverSocket.accept();
				
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
				dbHandler.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//메인메소드 : Server객체를 생성하고 초기화한다
	public static void main(String[] args) {
		new MultiServer().init();
		
	}
	
	//접속된 모든 클라이언트에게 서버의 메세지를 echo해줌
	public void sendAllMsg(String name, String msg) {
		
		//Map에 저장된 객체의 키값(이름)을 먼저 얻어온다.
		Iterator<String> it	= clientMap.keySet().iterator();
		
		//저장된 객체의 (클라이언트)갯수만큼 반복한다.
		while (it.hasNext()) {
			try {
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				
				PrintWriter it_out = 
						(PrintWriter)clientMap.get(it.next());
				
				//클라이언트에게 메세지를 전달한다.
				
				/*매개변수로 전달된이름이 없는경우에는 메세지만 echo한다.
					있는경우에는 이름+메세지를 전달한다.	*/
				
				
				if(name.equals("")) {
					//해쉬맵에 저장되어있는 클라이언트들에게 메세지를 전달한다.
					//따라서 접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				}
				else if( ) {
					it_out.println(msg); 
				}
				else {
					it_out.println("["+name+"]: " + msg);
				}
			} catch (Exception e) {
				System.out.println("예외3: "+ e);
				e.printStackTrace();
			}
			
		}
	}
			
	//내부크래스
	class MultiServerT extends Thread {
		
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		//생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(
						this.socket.getOutputStream(), true);
				in = new BufferedReader(
						new InputStreamReader(
								this.socket.getInputStream(), "UTF-8"));
			} catch (Exception e) {
				System.out.println("예외:"+ e);
			}
		}
		
		@Override
		public void run() {
			
			String name = "";
			String s = "";
			
			try {
				
				name = in.readLine();
				name = URLDecoder.decode(name, "UTF-8");
				
				sendAllMsg("", name+" 님이 입장하셨습니다.");
				
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				dbHandler.execute(name, "[입장]");
				
				//HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println(
						"현재 접속자수는"+ clientMap.size()+"명입니다.");
				
				
				//입력한 메세지는 모든 클라이언트에게 echo된다.
				//클라이언트의 메세지를 읽어온후 콘솔에 출력하고 echo한다.
				while (in != null) {
					s = in.readLine();
					s = URLDecoder.decode(s, "UTF-8");
					
					
					if(s == null) break;
					
					System.out.println(name + " >>"+ s);
					sendAllMsg(name, s);
					dbHandler.execute(name, s);
				}
			} catch (Exception e) {
				System.out.println("예외: "+ e);
			}
			finally {
				/*
				클라이언트가 접속을 종료하면 예외가 발생하게 되어 finally로 넘어온다.
				이때 대화명을 통해 해당 객체를 찾아 remove()시킨다.
				 */
				clientMap.remove(name);		
				sendAllMsg("", name+"님이 퇴장하셨습니다.");
				dbHandler.execute(name, "[퇴장]");
				
				//퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println(
						name+"["+Thread.currentThread().getName()+"] 퇴장");
				
				System.out.println("현재 접속자수는 "+clientMap.size()+"명입니다.");
				
				try {
					in.close();
					out.close();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		
	}//내부클래스 : MultiServerT


	
	
}

//대화내용을 DB에 저장할수있도록. 대화명 + 대화내용 + 현재 시각


























