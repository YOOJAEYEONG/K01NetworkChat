package chat6jdbc;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer {

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
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
			serverSocket = new ServerSocket(9999, 5);
			System.out.println("서버가 시작되었습니다.");

			/*
			클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 
			쓰레드 생성및 start.
			*/
			while (true) {
				System.out.println(1);
				socket = serverSocket.accept();
				System.out.println(2);
				Thread mst = new MultiServerT(socket);
				System.out.println(3);
				mst.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	//메인메소드 : Server객체를 생성하고 초기화한다
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();
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
					있는경우에는 이름+메세지를 전달한다.
				*/
				
				if(msg.indexOf("광고")>-1) {
					it_out.println("금지문구이므로 출력되지 않음");
				}else {
					if(name.equals("")) {
						//해쉬맵에 저장되어있는 클라이언트들에게 메세지를 전달한다.
						//따라서 접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
						it_out.println(msg);
					}
					else {
						it_out.println("["+name+"]: "+msg);
					}					
				}
				
			} catch (Exception e) {
				System.out.println("예외3: "+ e);
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
								this.socket.getInputStream()));
			} catch (Exception e) {
				System.out.println("예외:"+ e);
			}
		}
		
		@Override
		public void run() {
			
			//클라이언트로부터 전송된 "대화명"을 저장할 변수
			String name = "";
			//메세지 저장용 변수
			String s = "";
			
			try {
				//클라이언트의 이름을 읽어와서 저장
				name = in.readLine();
				System.out.println("바로 출력:"+name);
				//readUTF()
				//접속한 클라이언트에게 새로운 사용자의 입장을 알림.
				//이 접속자는 아직 해쉬맵에 저장되기 전이므로 
				//접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
				sendAllMsg("", name+" 님이 입장하셨습니다.");
				
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				
				//HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println(
						"현재 접속자수는"+ clientMap.size()+"명입니다.");
				
				//접속자수 2명으로 제한
				if(clientMap.size()>2) {
					System.out.println("접속자수 제한을 초과하였습니다.");
				}
				
				//입력한 메세지는 모든 클라이언트에게 echo된다.
				//클라이언트의 메세지를 읽어온후 콘솔에 출력하고 echo한다.
				while (in != null) {
					s = in.readLine();
					if(s == null) break;
					
					
					String query = "insert into member values"
							+ "	(seq_bbs_num.nextval,?,?,sysdate)";
					System.out.println("대화 입력:"+query);
					
					
					System.out.println(name + " >>"+ s);
					sendAllMsg(name, s);
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
		
		
	}
}


























