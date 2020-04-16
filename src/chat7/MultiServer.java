package chat7;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/*
MultiServer
	ㄴsendAllMsg() - toName 호출 시도
	MultiServerT
		Commands
			ㄴtoName
				
				
 */
public class MultiServer {

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	DBHandler dbHandler;
	MultiServerT serverT;//널임
	Thread mst;
	
	
	//클라이언트 정보 저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap= new HashMap<String, PrintWriter>();;
	
	//클라이언트의 IP주소를 저장할 set컬렉션
	HashMap<Integer,InetAddress> blacklist = new HashMap<Integer,InetAddress>();;
	HashMap<Integer,InetAddress> whitelist  = new HashMap<Integer,InetAddress>();;

	//귓속말 그룹
	HashSet<String> whisperSet = new HashSet<String>();
	
	//차단한 그룹
	HashSet<String> quietSet = new HashSet<String>();
	
	InetAddress address;
	
	HashSet<String> banWords = new HashSet<String>();
	
	
	
	
	
	
	public MultiServer() {
		
		//HashMap동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는것을 차단한다.
		Collections.synchronizedMap(clientMap);
		
		
		
		banWords.add("바보");
	}
	
	//서버 초기화
	public void init() {
		try {
			serverSocket = new ServerSocket(9999);
			//서버시작후 DB테이블 생성 및 연동 준비
			dbHandler = new DBHandler();
			
			int whitelistKey = 1;
			
			while (true) {				
				socket = serverSocket.accept();
				System.out.println( "신규접속IP"+socket.getInetAddress() );
				
				address = socket.getInetAddress();
				whitelist.put(whitelistKey++, address);
				
				
				if(blacklist.containsValue(address)) {
					socket = null;
					System.out.println(address+"는 차단되었습니다." );
					continue;
				}
				
				
				mst = new MultiServerT(socket);
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
	public void sendAllMsg(String name, String msg, String toName) {
		
		//Map에 저장된 객체의 키값(이름)을 먼저 얻어온다.
		Iterator<String> it	= clientMap.keySet().iterator();
		
		while (it.hasNext()) {
			try {
				//각 클라이언트의 PrintWriter객체를 얻어온다.
				String who = it.next();
				PrintWriter it_out = clientMap.get(who);
				
				
				/*매개변수로 전달된이름이 없는경우에는 메세지만 echo한다.
					있는경우에는 이름+메세지를 전달한다.	*/
				if(name.equals("")) {
					//해쉬맵에 저장되어있는 클라이언트들에게 메세지를 전달한다.
					//따라서 접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
					System.out.println("1번");
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				}
				else if(name.equals(who) ) {
					//보낸사람과 보낼대상(who)과  같은경우 echo 설정
					System.out.println("2번");
					it_out.println(msg); 
				}
				else if(quietSet.contains(toName) ) {
					//차단한 사람이 보내는 메세지일경우
					System.out.println("3번");
					clientMap.get(who).println(who+"에게 전송이 실패하였습니다.");
				}
				else if(whisperSet.contains(who) ) {
					//귓속말을을 설정한 사람이 있는 경우
					System.out.println("4번");
					clientMap.get(who).println("["+name+"] : " + msg);
				}
				else {
					//전체 한테 보내는 메세지
					System.out.println("5번");
					clientMap.get(who).println("["+name+"] : " + msg);
				}
				
			} catch (Exception e) {
				System.out.println("예외3: "+ e);
				e.printStackTrace();
			}
			
		}
	}
			
	public String checkDuplicate(String name) {
		int addName = 1;
		String copy = name;
		while(clientMap.containsKey(copy)){	
			System.out.println("중복발견");
			copy += addName++;
			
		}
		System.out.println("중복체크 최종결과  "+copy);//ee1
		return copy;
			
		
	}
	
	
	
	public String banWords(String str) {
	
		Iterator<String> it = banWords.iterator();
		String text=str;
		while(it.hasNext()) {
			String word = it.next();
			if(str.contains(word)) {
				text = str.replace(word, "***");
			}
			
		}

		return text;
	}
	
	
	//내부크래스
	public class MultiServerT extends Thread {
		
		Scanner scan = new Scanner(System.in);
		
		
		//멤버변수
		Socket socket;
		public rintWriter out = null;
		BufferedReader in = null;
		
		String name = "";
		String s = "";
		String[] msgArr;
		String order;
		String toName = "";
		boolean stoper = false;
		
		public MultiServerT() {}
		
		
		//생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(
						this.socket.getOutputStream(), true);
				
				in = new BufferedReader(new InputStreamReader(
								this.socket.getInputStream(), "UTF-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		
		@Override
		public void run() {
			
			
			
			
			try {
				name = in.readLine();
				name = URLDecoder.decode(name, "UTF-8");
				name = checkDuplicate(name);
				
				
				
				
				
				sendAllMsg("", name+" 님이 입장하셨습니다.","");
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				dbHandler.execute(name, "[입장]");
				
				//HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println("현재 접속자수는"+ clientMap.size()+"명입니다.");
				
				
				
				
				//입력한 메세지는 모든 클라이언트에게 echo된다.
				//클라이언트의 메세지를 읽어온후 콘솔에 출력하고 echo한다.
				while (in != null) {
					s = in.readLine();
					s = URLDecoder.decode(s, "UTF-8");
					
					dbHandler.execute(name, s);
					System.out.println(name + "> "+ s);
					
					
					
					msgArr = s.split(" ");
					order = msgArr[0];
					toName = (msgArr.length>=2) ? msgArr[1] : "";
					
					//공백으로 쪼개진 스트링 배열의 메세지 부분을 다시 합침
					for(int i=2 ; i<msgArr.length ; i++) {
						s = s.concat(" "+msgArr[i]);
					}
					
					
					
					
					//대화 금칙어 필터링
					s = banWords(s);
					
					if(s == null ) break;
					
					
					if(s.length()>1 && s.charAt(0)=='/') {
						
						
						commands(name, s, toName, order);
						
						if(stoper==true)
							continue;
						
					}
					
					
					sendAllMsg(name, s, toName);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				
				clientMap.remove(name);		
				sendAllMsg("", name+"님이 퇴장하셨습니다.","");
				dbHandler.execute(name, "[퇴장]");
				
				//퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println("["+name+"] 퇴장");
				
				System.out.println("현재 접속자수는 "+clientMap.size()+"명입니다.");
				
				try {
					in.close();
					out.close();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			

			
			
			
		}//run()
		
		void commands(String name, String s, String toName, String order) {

			switch (order) {
			case "/help":
				System.out.println("/list : 접속자 리스트 출력");
				System.out.println("/to [이름] [메세지]: [이름]에게 귓속말 보내기");
				System.out.println("/to [이름] : [이름]에게 귓속말 고정/해제");
				System.out.println("/quiet [이름] : [이름]의 대화 차단/해제");
				System.out.println("/addblacklist: 블랙리스트에 해당IP 추가");
				System.out.println("/showblacklist: 저장된 블랙리스트 확인");
			break;
			case "/list":
				System.out.println(clientMap.keySet()+"111");
				showUserList();
				
				break;
			case "/to":
				whisper(); 	break;
			case "/quiet":
				quiet();
				break;
			case "/addblacklist":
				addBlackList();
				break;
			case "/showblacklist":
				showBlackList();
				break;
			default:
				System.out.println("잘못된 명령어입니다.");
				System.out.println("/help : 명령어 보기");
				
				break;
			}
			
		}
		
		
		
		public void showUserList() {
			
			
			Iterator<String> it = clientMap.keySet().iterator();
			
			while (it.hasNext()) {
				
				clientMap.get(name).println(it.next());
				
			}
			
			order = null;
			
			
		}
		
		void whisper() {
			
			if(s.toString().equals("")) {
				if(whisperSet.add(toName)) {
					clientMap.get(name).println(toName+"에게 귓속말 고정 설정");
				}
				else if(whisperSet.remove(toName)){
					clientMap.get(name).println(toName+"에게 귓속말 고정 해제");
				}
			}
			else {//	/to 홍길동 안녕하세요
				//clientMap.get(toName).println("["+name+"] : " + s);
				stoper = true;
			}
		}
		
//		void whisper(String s) {
//			clientMap.get(toName).println("["+name+"] : " + s);
//		}
		
		void quiet() {
			
			if(quietSet.add(toName)) {
				clientMap.get(name).println(toName+"차단 설정");
			}
			else if(quietSet.remove(toName)) {
				clientMap.get(name).println(toName+"차단 해제");
			}
			
			
			/*
			Iterator<String> it = server.clientMap.keySet().iterator();
			while(it.hasNext()) {
				
				String who = it.next();
				PrintWriter it_out = (PrintWriter)server.clientMap.get(who);
				
				
				if(toName.equals(who) && msg.toString().equals("") ) {
					
					
					
					
					
					if(serverT.setQuiet==true) {
						server.clientMap.get(commander).println(toName+"의 대화 차단");
						serverT.toQuiet = toName;
					}
					if(serverT.setQuiet==false) {
						server.clientMap.get(commander).println(toName+"의 대화 허용");
						serverT.toQuiet = "";
					}
				}
				
				


				if(commander.equals(who) ) {
					//보낸사람과 보낼대상(who)과  같은경우 echo 설정
					it_out.println(msg); 
				}
				else if(toName.equals(who)) {
					//특정 사용자 차단.
					
					server.clientMap.get(commander).println("["+toName+"] : " + "***");
					
					
				}
				else {
					it_out.println("["+commander+"]: " + msg);
				}
			}
			*/
		}
		
		void quiet(String s) {
			clientMap.get(toName).println("["+name+"] : " + s);
		}
		
		
		void addBlackList() {
			
			System.out.println("추가할 IP선택");
			
			int keyW = whitelist.size();
			while(keyW > 0) {
				System.out.println(keyW+"번"+whitelist.get(keyW--));
			}
			
			System.out.println("차단설정할 IP주소를 선택하시오");
			int input = scan.nextInt();
			InetAddress address = whitelist.get(input);
			int nextkey = blacklist.size();
			System.out.println("블랙리스트추가 "+ blacklist.put(++nextkey, address));
			
		}
		
		void showBlackList() {
			int keyB = blacklist.size();
			while(0<keyB) {
				System.out.println(keyB+"번"+whitelist.get(keyB--));
			}
		}
		
		

		
	}//내부클래스 : MultiServerT

	
	
}



























