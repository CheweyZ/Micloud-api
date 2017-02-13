package info.micloud.api.httpserver;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import info.micloud.api.apipages.APIPages;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;

public class MiCloudHTTPServer extends SimpleChannelInboundHandler<Object> {
	
	final APIPages apiPages;
	public MiCloudHTTPServer(){
		apiPages = new APIPages();
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	HttpRequest currentRequest;
	StringBuilder outputBuffer = new StringBuilder();
	
	@SuppressWarnings("deprecation")
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof HttpRequest) {
			currentRequest = (HttpRequest) msg;
			if (currentRequest.getUri().startsWith("/.well-known")){
				send302Redirect(ctx, currentRequest.getUri());
				return;
			}
			if (HttpUtil.is100ContinueExpected(currentRequest)) {
                send100Continue(ctx);
            }
			outputBuffer.setLength(0);
			//writeResponse(currentRequest, ctx, currentRequest, "<h1>testing</h1><form action='me.php' method='post'><input name='key'><input type='submit'></form>".getBytes());
		}
		if (msg instanceof HttpContent){            
			if (msg instanceof LastHttpContent) {
				HttpContent httpContent = (HttpContent) msg;
				
				String cookieString = currentRequest.headers().get(HttpHeaderNames.COOKIE);
				Set<Cookie> cookies = null;
				String sessionId = "";
		        if (cookieString != null) {
		            cookies = ServerCookieDecoder.STRICT.decode(cookieString);
		            for (Cookie cookie : cookies){
		            	if (cookie.name().equals("sessionId")){
		            		sessionId = cookie.value();
		            	}
		            }
		        } else {
		        	cookies = new HashSet<>();
		        }

	            ByteBuf content = httpContent.content();
	            if (content.isReadable()) { //POST body
	            	QueryStringDecoder queryStringDecoder = new QueryStringDecoder("?"+content.toString(CharsetUtil.UTF_8));
	                Map<String, List<String>> params = queryStringDecoder.parameters();
	                apiPages.pageLoaded(sessionId, currentRequest.getUri(), params, currentRequest, ctx);
	            } else {
	            	apiPages.pageLoaded(sessionId, currentRequest.getUri(), new HashMap<String, List<String>>(), currentRequest, ctx);
	            }
			}
		}
	}
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }
    
    private static void send302Redirect(ChannelHandlerContext ctx, String uri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FOUND,
        		Unpooled.copiedBuffer("Redirecting", CharsetUtil.UTF_8));
        String newLoc = "https://micloud.info"+uri;
        System.out.println("Redirecting "+uri+" to "+newLoc);
        response.headers().set(HttpHeaderNames.LOCATION, newLoc);
        ctx.write(response);
    }
}
