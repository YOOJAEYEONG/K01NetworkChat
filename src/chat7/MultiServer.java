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
	HashSet<String> whisperSet;
	HashMap<String,String> quietMap;
	
	
	
	//클라이언트 정보 저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap= new HashMap<String, PrintWriter>();;
	
	//클라이언트의 IP주소를 저장할 set컬렉션
	HashMap<Integer,InetAddress> blacklist = new HashMap<Integer,InetAddress>();;
	HashMap<Integer,InetAddress> whitelist  = new HashMap<Integer,InetAddress>();;

	
	
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
	public void sendAllMsg(String name, String msg, String toName, boolean allmsg) {
		
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
				if(name.equals(who) ) {
					//보낸사람과 보낼대상(who)과  같은경우 (내가 쓴글 나한테 표시)
					System.out.println("2번");
					it_out.println(msg); 
				}
				else if(whisperSet.contains(who) ) {
					//귓속말을을 설정한 사람이 있는 경우
					System.out.println("4번");
					clientMap.get(who).println("["+name+"] : " + msg);
					allmsg=false;
				}
				else if(quietMap.contains(name)) {
					//차단한 사람이 보내는 메세지일경우
					System.out.println("3번");
					clientMap.get(name).println(who+"에게 전송이 실패하였습니다.");
				}
				else if(allmsg==true){
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
		
		
		
		
		
		
		
		//멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		String name = "";
		String s = "";
		String[] msgArr;
		String order;
		String toName = "";
		String msg = "";
		boolean allmsg = true;
		
		Scanner scan = new Scanner(System.in);
		
		
		
		public MultiServerT() {}
		
		
		//생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			
			//귓속말 그룹
			whisperSet = new HashSet<String>();
			
			//차단한 그룹
			quietMap = new HashMap<String,String>();
			
			
			
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
				
				
				
				
				
				sendAllMsg(name, "님이 입장하셨습니다.","", true);
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
					
					//대화 금칙어 필터링
					s = banWords(s);
					
					if(s == null ) break;
					if(s.length()>1 && s.charAt(0)=='/') {
						System.out.println("명령문이 입력됨");
						msgArr = s.split(" ");
						order = msgArr[0];
						toName = (msgArr.length>=2) ? msgArr[1] : "";
						msg = (msgArr.length>=3) ? msgArr[2] : "";
						
						//공백으로 쪼개진 스트링 배열의 메세지 부분을 다시 합침
						for(int i=3 ; i<msgArr.length ; i++) {
							msg = s.concat(" "+msgArr[i]);
						}
						
						commands(name, msg, toName, order);			
					}
					
					
					sendAllMsg(name, s, toName, allmsg);
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				
				clientMap.remove(name);		
				sendAllMsg("", name+"님이 퇴장하셨습니다.","", true);
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
		
		void commands(String name, String msg, String toName, String order) {

			switch (order) {
			case "/help":
				allmsg = false;
				System.out.println("/list : 접속자 리스트 출력");
				System.out.println("/to [이름] [메세지]: [이름]에게 귓속말 보내기");
				System.out.println("/to [이름] : [이름]에게 귓속말 고정/해제");
				System.out.println("/quiet [이름] : [이름]의 대화 차단/해제");
				System.out.println("/addblacklist: 블랙리스트에 해당IP 추가");
				System.out.println("/showblacklist: 저장된 블랙리스트 확인");
			break;
			case "/list":
				showUserList();	break;
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
		
		public void help() {
			clientMap.get(name).println("/list : 접속자 리스트 출력");
			clientMap.get(name).println("/to [이름] [메세지]: [이름]에게 귓속말 보내기");
			clientMap.get(name).println("/to [이름] : [이름]에게 귓속말 고정/해제");
			clientMap.get(name).println("/quiet [이름] : [이름]의 대화 차단/해제");
			clientMap.get(name).println("/addblacklist: 블랙리스트에 해당IP 추가");
			clientMap.get(name).println("/showblacklist: 저장된 블랙리스트 확인");
		}
		
		public void showUserList() {
			
			
			Iterator<String> it = clientMap.keySet().iterator();
			
			while (it.hasNext()) {
				
				clientMap.get(name).println(it.next());
				
			}
			
			allmsg = false;
			
			
		}
		
		void whisper() {
			if(msg.equals("")) {
				System.out.println("msg가없을때");
				if(whisperSet.add(toName)) {
					clientMap.get(name).println(toName+"에게 귓속말 고정 설정");
					clientMap.get(name).println("귓속말 고정 목록 : "+whisperSet);
					allmsg = false;
				}
				else if(whisperSet.remove(toName)){
					clientMap.get(name).println(toName+"에게 귓속말 고정 해제");
					clientMap.get(name).println("귓속말 고정 목록 : "+whisperSet);
					
					allmsg = true;
				}
			}
			else {//	/to 홍길동 안녕하세요
				
				clientMap.get(toName).println("["+name+"] : " + msg);
				allmsg = false;
				
			}
		}
		

		
		void quiet() {
			
			if(quietMap.put(name,toName)) {
				clientMap.get(name).println(toName+"차단 설정");
				clientMap.get(name).println("차단된 사용자 목록:"+quietMap);
				allmsg = false;
			}
			else if(quietMap.remove(toName)) {
				clientMap.get(name).println(toName+"차단 해제");
				clientMap.get(name).println("차단된 사용자 목록:"+quietSet);
				allmsg = true;
			}
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

	class Blacklist {
		String blocker; //차단한 사람
		String blocked; //차단된 사람
	}
	
	
}



























