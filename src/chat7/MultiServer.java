package chat7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;


public class MultiServer {

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	
	InetAddress address;
	DBHandler dbHandler;
	
	//귓속말을 하는대상을 저장
	HashSet<Relation> whisperSet= new HashSet<Relation>();
	//차단한 대상을 저장함
	HashSet<Relation> quietSet= new HashSet<Relation>();
	
	
	
	//클라이언트 정보 저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap= new HashMap<String, PrintWriter>();
	
	//클라이언트의 IP주소를 저장할 set컬렉션
	HashMap<Integer,InetAddress> blacklist = new HashMap<Integer,InetAddress>();
	HashMap<Integer,InetAddress> whitelist  = new HashMap<Integer,InetAddress>();
	//금칙어를 설정한 단어들을 저장
	HashSet<String> banWords = new HashSet<String>();
	
	
	
	
	
	
	public MultiServer() {
		
		//HashMap동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는것을 차단한다.
		Collections.synchronizedMap(clientMap);
		
		banWords.add("엿");
		banWords.add("멍청이");
		banWords.add("바보");
	}
	
	//서버 초기화
	public void init() {
		try {
			
			
			serverSocket = new ServerSocket(9999);
			//서버시작후 DB테이블 생성 및 연동 준비
			dbHandler = new DBHandler();
			
			
			while (true) {				
				socket = serverSocket.accept();
				System.out.println( "신규접속IP"+socket.getInetAddress() );
				
				address = socket.getInetAddress();
				whitelist.put(whitelist.size(), address);
				
				
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
				
				if(name.equals(who) ) {
					//보낸사람과 보낼대상(who)과  같은경우 (내가 쓴글 나한테 표시)
					System.out.println("1번");
					clientMap.get(who).println(msg); 
				}
				else if(whisperSet.size()!=0 && who.equals(toName)) {
					//귓속말을을 설정한 사람이 있는 경우
					System.out.println("2번");
					Relation test = new Relation(name, who);
					
					if(whisperSet.add(test)) {
						whisperSet.remove(test);
						allmsg=true;
					}
					else {
						clientMap.get(who).println("["+name+"] : " + msg);
						allmsg=false;
					}
				}
				else if(quietSet.size()!=0){
					//차단한 사람이 보내는 메세지일경우
					System.out.println("3번");
					Relation test = new Relation(who, name);
					
					if(quietSet.add(test)) {
						quietSet.remove(test);
						clientMap.get(who).println("["+name+"] : " + msg);
					}
					else {
						clientMap.get(name).println(
								who+"에게 전송이 실패하였습니다.");
					}
				}
				else if(allmsg==true){
					//전체 한테 보내는 메세지
					System.out.println("4번");
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
		System.out.println("중복체크 최종결과  "+copy);
		return copy;
			
		
	}
	
	
	
	public String banWords(String str) {
	
		Iterator<String> it = banWords.iterator();
		String text=str;
		while(it.hasNext()) {
			String word = it.next();
			if(str.contains(word)) {
				text = str.replace(word, "**");
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
		String msg = "";
		
		String[] msgArr;
		String order;
		String toName = "";
		boolean allmsg;
		Scanner scan = new Scanner(System.in);
		
		
		
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
				
				
				sendAllMsg(name, "님이 입장하셨습니다.","", true);
				//현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				dbHandler.execute(name, "[입장]");
				
				//HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println(
						"현재 접속자수는"+ clientMap.size()+"명입니다.");
				
				
				//클라이언트의 메세지를 읽어온후 콘솔에 출력하고 echo한다.
				while (in != null) {
					allmsg = true;
					msg = in.readLine();
					msg = URLDecoder.decode(msg, "UTF-8");
					
					dbHandler.execute(name, msg);
					System.out.println(name + "> "+ msg);
					
					//대화 금칙어 필터링
					msg = banWords(msg);
					
					if(msg == null ) break;
					if(msg.length()>1 && msg.charAt(0)=='/') {
						System.out.println("명령문이 입력됨");
						msgArr = msg.split(" ");
						
						order = msgArr[0];
						toName = (msgArr.length>=2) ? msgArr[1] : "";
						msg = (msgArr.length>=3) ? msgArr[2] : "";
						
						//공백으로 메세지까지 쪼개진 스트링 배열의 메세지 부분을 다시 합침
						for(int i=3 ; i<msgArr.length ; i++) {
							msg = msg.concat(" "+msgArr[i]);
						}
						
						commands(name, msg, toName, order);			
					}
					
					sendAllMsg(name, msg, toName, allmsg);
					
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
				help();			break;
			case "/list":
				showUserList();	break;
			case "/to":
				whisper(); 		break;
			case "/quiet":
				quiet();		break;
			case "/addblacklist":
				addBlackList();		break;
			case "/showblacklist":
				showBlackList();	break;
			default:
				clientMap.get(name).println("잘못된 명령어입니다.");
				clientMap.get(name).println("/help : 명령어 보기");
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
				Relation set = new Relation(name, toName);
				if(whisperSet.add(set)) {
					clientMap.get(name).println(toName+"에게 귓속말 고정 설정");
					clientMap.get(name).println("귓속말 고정 목록 : "+whisperSet);
					allmsg = false;
				}
				else if(whisperSet.remove(set)){
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
			Relation relation = new Relation(name, toName);
			
			if(quietSet.add(relation)) {
				clientMap.get(name).println(toName+"차단 설정");
				clientMap.get(name).println("차단된 사용자 :"+quietSet.size());
				allmsg = false;
			}
			else if(quietSet.remove(relation)) {
				clientMap.get(name).println(toName+"차단 해제");
				clientMap.get(name).println("차단된 사용자 :"+quietSet.size());
				allmsg = true;
			}
		}
		

		
		void addBlackList() {
			
			String i=null;;
			
			clientMap.get(name).println("추가할 IP선택");
			int key = 0;
			while(key<whitelist.size()) {
				clientMap.get(name).println(key+"번"+whitelist.get(key++));
			}
			
			clientMap.get(name).println("차단설정할 IP주소를 선택하시오");
			try {
				i = in.readLine();
				i = URLDecoder.decode(i, "UTF-8");
			} catch (IOException e) { }
			blacklist.put(blacklist.size(), address);
			InetAddress address = whitelist.get(Integer.parseInt(i));
			clientMap.get(name).println("블랙리스트추가 "+ address );
			
		}
		
		void showBlackList() {
			int key = 0;
			while(key<=blacklist.size()) {
				clientMap.get(name).println(key+"번"+blacklist.get(key++));
			}
		}
		
		

		
	}//내부클래스 : MultiServerT

	class Relation {
		
		String by; //설정한 사람
		String to; //설정된 사람
		
		public Relation(String by, String to) {
			this.by = by;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((by == null) ? 0 : by.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Relation other = (Relation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (by == null) {
				if (other.by != null)
					return false;
			} else if (!by.equals(other.by))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}

		private MultiServer getOuterType() {
			return MultiServer.this;
		}

		public String getBy() {
			return by;
		}

		
		
	}
	
	
}



























