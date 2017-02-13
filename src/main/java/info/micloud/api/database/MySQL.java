package info.micloud.api.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

	private final String user;
	private final String password;
	private final int port;
	private final String ip;
	private final String database;

	public MySQL(String ip, int port, String username, String password, String database) {
		this.ip = ip;
		this.port = port;
		this.user = username;
		this.password = password;
		this.database = database;
	}

	public Connection openConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); //make sure driver is initialised
			return DriverManager.getConnection("jdbc:mysql://" + this.ip + ":" + this.port + "/" + this.database
					+ "?useUnicode=true&characterEncoding=utf-8", this.user, this.password);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}