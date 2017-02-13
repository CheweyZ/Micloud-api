package info.micloud.api.apipages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import info.micloud.api.database.ConnectionPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

public class PageWhoAmI extends APIPage {

	public PageWhoAmI() {
		super("whoami", false);
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			PreparedStatement statement = connection.prepareStatement("SELECT `data`, `firstName`, `lastName`, `organisation` FROM `users` WHERE `userid`=?;");
			statement.setInt(1, userId);
			ResultSet set = statement.executeQuery();
			if (set.next()){
				String firstName = set.getString("firstName");
				String lastName = set.getString("lastName");
				String organisation = set.getString("organisation");
				JSONObject response = new JSONObject();
				response.put("firstName", firstName);
				response.put("lastName", lastName);
				response.put("organisation", organisation);
				response.put("loggedin", 1);
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, response);
				set.close();
				return;
			} else {
				set.close();
				JSONObject error = new JSONObject();
				error.put("loggedin", 0);
				writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, error);
				return;
			}
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
}
