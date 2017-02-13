package info.micloud.api.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import info.micloud.api.MiCloud;

/**
 * 
 * @author alex
 * The connection pool will only have as many threads as the netty webserver has worker threads
 * (as defined in the configuration file)
 */
public class ConnectionPool {

	/**
	 * java.sql.Connection, Boolean
	 * Connection: Database connection
	 * Boolean: Whether the connection is in use (true = allocated)
	 */
	private static ConcurrentHashMap<Connection, Boolean> pool = new ConcurrentHashMap<>();
	
	public static Connection allocateConnection(){
		Connection connection = null;
		for (Map.Entry<Connection, Boolean> entry : pool.entrySet()){
			if (entry.getValue() == false){
				connection = entry.getKey();
				break;
			}
		}
		if (connection == null){//an unallocated connection wasn't found, open a new one
			connection = openConnection();
		}
		
		int tests = 0;
		while (!testConnection(connection)){
			tests++;
			pool.remove(connection);
			connection = openConnection();
			if (tests > 5){
				System.err.println("DATABASE CONNECTION FAILED "+new Date().toString());
				break;
			}
		}
		pool.put(connection, true); //allocate the connection
		return connection;
	}
	
	public static Connection openConnection(){
		Connection connection = MiCloud.getInstance().getSqlCredentials().openConnection();
		pool.put(connection, false); //unallocated
		return connection;
	}
	
	public static void unallocateConnection(Connection connection){
		pool.put(connection, false);
	}
	
	public static boolean testConnection(Connection connection){
		if (connection == null){
			return false;
		}
		try {
			//see if the client closed it, then ping it with a timeout of 1 second
			if (connection.isClosed() || !connection.isValid(1)){
				return false;
			}
		} catch (SQLException e) {
			return false;
		}
		
		return true;
	}
	
	public static void closeAllConnections(){
		Enumeration<Connection> connections = pool.keys();
		while (connections.hasMoreElements()){
			Connection connection = connections.nextElement();
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace(); //not very nice to print out lots of stacktraces when
				//multiple connctions fail to close (e.g. network disconnected)
			}
		}
	}
}
