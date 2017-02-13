package info.micloud.api.apipages;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import info.micloud.api.database.ConnectionPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

public class PageLogin extends APIPage {

	MessageDigest md5 = null;
	SecureRandom random = new SecureRandom();
	public PageLogin() {
		super("login", false);
		
		try {
			this.md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		if (userId != -1){
			JSONObject error = new JSONObject();
			error.put("error", "400 Bad request");
			error.put("debug", "User already authenticated");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
			return;
		}
		if (params.containsKey("username") && params.containsKey("password")){
			Connection connection = ConnectionPool.allocateConnection();
			try {
				PreparedStatement statement = connection.prepareStatement("SELECT `userid`, `password`, `firstName`, `lastName`, `organisation` FROM `users` WHERE `username`=?;");
				statement.setString(1, params.get("username").get(0));
				ResultSet set = statement.executeQuery();
				if (set.next()){
					String passwordHash = set.getString("password");
					String requestedPasswordHash = bytesToHex(md5.digest(params.get("password").get(0).getBytes(CharsetUtil.UTF_8)));
					if (!passwordHash.equals(requestedPasswordHash)){
						set.close();
						JSONObject error = new JSONObject();
						error.put("boxType", "error");
						error.put("loggedin", 0);
						error.put("message", "Bad username/password"); //bad password
						writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.OK, error);
						return;
					} else {
						int userid = set.getInt("userid");
						String id = generateSessionId(connection);
						
						//prepare name for the whois response
						String firstName = set.getString("firstName");
						String lastName = set.getString("lastName");
						String organisation = set.getString("organisation");
						
						
						set.close();
						//authorise the sessionId
						Timestamp expiry = new Timestamp(System.currentTimeMillis() + 604800000); //+milliseconds in 7 days
						String userAgent = "";
						if (currentRequest.headers().contains("User-Agent")){
							userAgent = currentRequest.headers().get("User-Agent");
						}
						
						statement = connection.prepareStatement("INSERT INTO `sessions` (sessionId, userId, expiry, userAgent) VALUES(?, ?, ?, ?);");
						//TODO can't get user's IP when apache is a proxy
						statement.setString(1, id);
						statement.setInt(2, userid);
						statement.setTimestamp(3, expiry);
						statement.setString(4, userAgent);
						statement.executeUpdate();
						statement.close();
						
						JSONObject response = new JSONObject();
						response.put("firstName", firstName);
						response.put("lastName", lastName);
						response.put("organisation", organisation);
						response.put("loggedin", 1);
						writeResponse(ctx, currentRequest, HttpResponseStatus.OK, response, id);
						return;
					}
				} else {
					set.close();
					JSONObject error = new JSONObject();
					error.put("boxType", "error");
					error.put("loggedin", 0);
					error.put("message", "Bad username/password"); //bad username
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
		} else {
			JSONObject error = new JSONObject();
			error.put("boxType", "error");
			error.put("message", "username and/or password not specified");
			writeSimpleResponse(ctx, currentRequest, HttpResponseStatus.BAD_REQUEST, error);
			return;
		}
	}
	
	private String bytesToHex(byte[] in) {
	    final StringBuilder builder = new StringBuilder();
	    for(byte b : in) {
	        builder.append(String.format("%02x", b));
	    }
	    return builder.toString();
	}
	
	private String generateSessionId(Connection connection){
		
		String id = newRandomSessionId();
		while (checkIfSessionIdExists(connection, id)){
			id = newRandomSessionId();
		}
		return id;
	}
	
	private String newRandomSessionId(){
		byte[] sessionIdBytes = new byte[16];
		random.nextBytes(sessionIdBytes);
		String id = Base64.getEncoder().encodeToString(sessionIdBytes);
		return id;
	}
	
	private boolean checkIfSessionIdExists(Connection connection, String id){
		try {
			ResultSet set = connection.prepareCall("SELECT `userId` FROM `sessions` WHERE `sessionId`='"+id+"';").executeQuery();
			if (set.next()){
				set.close();
				return true;
			} else {
				set.close();
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean writeResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus httpCode, JSONObject responseData, String sessionId) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, httpCode,
                Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        if (request.headers().contains("Origin")){
        	String origin = request.headers().get("Origin");
        	if (origin.endsWith("micloud.info")){
        		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        		response.headers().set(HttpHeaderNames.VARY, "Origin");
        	}
        }

        if (keepAlive) {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

       	DefaultCookie cookie = new DefaultCookie("sessionId", sessionId);
       	cookie.setMaxAge(604800);
       	cookie.setPath("/");
       	response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        
        ctx.write(response);

        return keepAlive;
    }
}
