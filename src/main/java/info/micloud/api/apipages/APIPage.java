package info.micloud.api.apipages;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;

public abstract class APIPage {
	
	final String pageName;
	final boolean needsAuthentication;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * 
	 * @param pageName: Lower case page name for URI, e.g. example.org/api/test, pageName would be 'test'
	 * @param needsAuthentication: Does user need to be logged in to request this? (false for login page)
	 */
	public APIPage(String pageName, boolean needsAuthentication){
		this.pageName = pageName;
		this.needsAuthentication = needsAuthentication;
	}
	
	public String getUriName(){
		return this.pageName;
	}
	
	public boolean getNeedsAuthentication(){
		return this.needsAuthentication;
	}
	
	public abstract void onLoad(String sessionId, int userId, Map<String, List<String>> params, HttpRequest currentRequest,
			ChannelHandlerContext ctx);
	
	public void writeSimpleResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus httpCode, JSONObject responseData) {
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
}
