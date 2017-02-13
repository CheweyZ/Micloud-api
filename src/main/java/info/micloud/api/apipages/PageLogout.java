package info.micloud.api.apipages;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

public class PageLogout extends APIPage {

	public PageLogout() {
		super("logout", true);
	}

	@Override
	public void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx) {
		Connection connection = ConnectionPool.allocateConnection();
		try {
			PreparedStatement statement = connection.prepareStatement("DELETE FROM `sessions` WHERE `sessionId`=?;");
			statement.setString(1, sessionId);
			statement.executeUpdate();
			statement.close();
			
			JSONObject status = new JSONObject();
			status.put("status", "success");
			writeResponseDeleteSessionId(ctx, currentRequest, HttpResponseStatus.OK, status);
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
