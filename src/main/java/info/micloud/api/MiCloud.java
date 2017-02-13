package info.micloud.api;

import info.micloud.api.database.ConnectionPool;
import info.micloud.api.database.MySQL;
import info.micloud.api.httpserver.HttpServer;
import info.micloud.api.main.Configuration;

public class MiCloud {

	private static MiCloud instance;
	
	public static MiCloud getInstance(){
		if (instance == null){
			instance = new MiCloud();
		}
		return instance;
	}
	private MySQL sqlCredentials;
	
	private void setupDatabase(){
		sqlCredentials = new MySQL(Configuration.MYSQL_IP, Configuration.MYSQL_PORT, Configuration.MYSQL_USERNAME, Configuration.MYSQL_PASSWORD, Configuration.MYSQL_DB);
		for (int i = 0;i < Configuration.THREAD_COUNT;i++){
			ConnectionPool.openConnection();
		}
	}
	
	public MySQL getSqlCredentials(){
		return this.sqlCredentials;
	}
	
	public void init(){
		setupDatabase();
		
		try {
			new HttpServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
