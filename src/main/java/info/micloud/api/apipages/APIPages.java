package info.micloud.api.apipages;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

public class APIPages {

	private ConcurrentHashMap<String, APIPage> pages = new ConcurrentHashMap<>();

	public APIPages() {
		List<APIPage> tempPages = new ArrayList<>();
		tempPages.add(new PageLogin());
		tempPages.add(new PageWhoAmI());
		tempPages.add(new PageLogout());
		tempPages.add(new PageNewPatient());
		tempPages.add(new PageNewStaff());
		tempPages.add(new PageSearchPatients());
		tempPages.add(new PageGetPatientProfile());
		tempPages.add(new PageAddVisit());

		for (APIPage page : tempPages) {
			pages.put(page.getUriName(), page);
		}
	}

	public void pageLoaded(String sessionId, String uri, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		String pageName = uri.substring(1).toLowerCase();
		if (pageName.indexOf("?") > 0){
			pageName = pageName.substring(0, pageName.indexOf("?"));
		}
		for (Map.Entry<String, APIPage> entry : pages.entrySet()){
			if (entry.getKey().equals(pageName)){
				boolean authenticated = false;
				int userId = -1;
				if (sessionId != null && !sessionId.equals("")){
					Connection connection = ConnectionPool.allocateConnection();
					try {
						PreparedStatement statement = connection.prepareStatement("SELECT * FROM `sessions` WHERE `sessionId`=?;");
						statement.setString(1, sessionId); //format String instead of using a stringbuilder to prevent SQL injection
						ResultSet set = statement.executeQuery();
						if (set.next()){
							Timestamp time = set.getTimestamp("expiry");
							if (new Date(System.currentTimeMillis()).after(time)){
								if (entry.getValue().getNeedsAuthentication()){
									JSONObject error = new JSONObject();
									error.put("boxType", "error");
									error.put("message", "403 Forbidden Access");
									writeResponseDeleteSessionId(ctx, currentRequest, HttpResponseStatus.FORBIDDEN, error);
								} else {
									sessionId = "";
								}
								
								set.close();
								//remove the expired session id
								statement = connection.prepareStatement("DELETE FROM `sessions` WHERE `sessionId`=?;");
								statement.setString(1, sessionId);
								statement.executeUpdate();
								statement.close();
								return;
							} else {
								authenticated = true;
								userId = set.getInt("userId");
								set.close();
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
						
						JSONObject error = new JSONObject();
						error.put("error", "500 Internal Server Error");
						error.put("debug", "SQLException while validating sessionId");
						writeResponse(ctx, currentRequest, HttpResponseStatus.INTERNAL_SERVER_ERROR, error);
						return;
					} finally {
						ConnectionPool.unallocateConnection(connection);
					}
				}
				if (!authenticated && entry.getValue().getNeedsAuthentication()){
					//not authenticated
					JSONObject error = new JSONObject();
					error.put("boxType", "error");
					error.put("message", "403 Forbidden Access");
					writeResponse(ctx, currentRequest, HttpResponseStatus.FORBIDDEN, error);
					return;
				} else {
					entry.getValue().onLoad(sessionId, userId, params, currentRequest, ctx);
					return;
				}
			}
		}
		
		JSONObject error = new JSONObject();
		error.put("boxType", "error");
		error.put("message", "404 Page not found");
		writeResponse(ctx, currentRequest, HttpResponseStatus.NOT_FOUND, error);
		return;
	}
	
	private void writeResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus httpCode, JSONObject responseData) {
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
        ctx.write(response);
    }
	
	private void writeResponseDeleteSessionId(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus httpCode, JSONObject responseData) {
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
        
        DefaultCookie cookie = new DefaultCookie("sessionId", "");
       	cookie.setMaxAge(0);
       	response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));

        if (keepAlive) {
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.write(response);
    }
}
