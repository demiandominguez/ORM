package tp.utn;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectDatabase {

	static ConnectDatabase instance;
	
	public static ConnectDatabase getInstance(){
		if(instance == null){
			instance = new ConnectDatabase();
		}
		return instance;
	}
	
	public Connection getConnection() {
		Connection con = null;

		try {
			Class.forName("org.hsqldb.jdbc.JDBCDriver") ;
			con = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/xdb", "SA" , "") ;
			if (con!= null) {
				System.out.println("Connection created successfully") ;

			}else{
				System.out.println("Problem with creating connection") ;
			}
		}  catch (Exception e) {
			e.printStackTrace(System.out) ;
		}
		return con;
	}
}