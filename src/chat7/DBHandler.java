package chat7;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;

public class DBHandler implements DBConnect{
	
	//동적쿼리를 위한 객체
	public Connection con;
	public PreparedStatement psmt;
	public ResultSet rs;
	public Statement stmt;
	
	public DBHandler() {
		
		try {
			//드라이버 로드
			Class.forName(ORACLE_DRIVER);
			//드라이버 연결
			con = DriverManager.getConnection(
					ORACLE_URL, ID, PASS);
			if(con!=null) {
				System.out.println("DB연결됨");
				//테이블 생성, 시퀀스 생성
				chatDBTable();
			}
			
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} 
	}
	
	
	
	public void chatDBTable() {
			try {
				String sqlCreateTable = 
						" CREATE TABLE chating_tb( " + 
								"	 seqNumber NUMBER PRIMARY KEY, " +
								"    name NVARCHAR2(20), " + 
								"    contents NVARCHAR2(100) , " + 
								"    time DATE DEFAULT SYSDATE " +
								" ) ";
				stmt = con.createStatement();
				rs = stmt.executeQuery(sqlCreateTable);
				System.out.println("chating_tb : 테이블생성됨");
				
				String sqlNewSequence = 
						"CREATE SEQUENCE seq_chating " + 
								"    increment by 1 " + 
								"    maxvalue 1000 " + 
								"    minvalue 1 " + 
								"    nocycle " + 
								"    nocache ";
				stmt = con.createStatement();
				rs = stmt.executeQuery(sqlNewSequence);
				System.out.println("seq_chating 시퀀스 생성됨");
				
			} catch (SQLSyntaxErrorException e) {
				System.out.println("기존 테이블을 계속사용합니다.");
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}
	
	public void close() {
		try {
			con.close();
			rs.close();
			stmt.close();
			psmt.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	
}
	
	
	
	
	
	



	

