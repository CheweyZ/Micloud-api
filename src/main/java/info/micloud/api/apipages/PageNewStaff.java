package info.micloud.api.apipages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import info.micloud.api.database.ConnectionPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageNewStaff extends APIPage {

	MessageDigest md5 = null;
	public PageNewStaff() {
		super("makestaffmember", true);
		try {
			this.md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			if (!params.containsKey("username") || !params.containsKey("firstName") || !params.containsKey("lastName")
					|| !params.containsKey("password") || !params.containsKey("organisation")){
				JSONObject error = new JSONObject();
				error.put("boxType", "error");
				error.put("message", "username, firstName, lastName, password and/or organisation not specified");
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
				return;
			}
			PreparedStatement statement = connection.prepareStatement("INSERT INTO `users` (username, firstName, lastName, password, organisation) VALUES(?, ?, ?, ?, ?);");
			statement.setString(1, params.get("username").get(0));
			String firstName = params.get("firstName").get(0);
			firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
			statement.setString(2, firstName);
			
			String lastName = params.get("lastName").get(0);
			lastName = lastName.substring(0, 1).toUpperCase() + lastName.substring(1).toLowerCase();
			statement.setString(3, lastName);
			
			String password = bytesToHex(this.md5.digest(params.get("password").get(0).getBytes()));
			statement.setString(4, password);
			
			statement.setString(5, params.get("organisation").get(0));
			
			statement.executeUpdate();
			statement.close();
			
			JSONObject response = new JSONObject();
			response.put("status", "success");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, response);
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			
			JSONObject error = new JSONObject();
			error.put("message", "500 - SQLException on page "+this.pageName);
			error.put("boxType", "error");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
			return;
		} finally {
			ConnectionPool.unallocateConnection(connection);
		}
	}
	
	private String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
}
