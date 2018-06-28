package example.nettypure

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

import io.netty.handler.codec.http.HttpHeaders.Names._
import io.netty.handler.codec.http.HttpHeaders._
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.HttpVersion._
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http._

object HttpHelloWorldServerHandler {
  private val CONTENT = "Hi from netty\n".getBytes(Charset.forName("us-ascii"))
}

class HttpHelloWorldServerHandler(requestCounter: AtomicInteger) extends ChannelInboundHandlerAdapter {
  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case req: HttpRequest =>
        requestCounter.incrementAndGet()
        if (HttpUtil.is100ContinueExpected(req)){
          ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE))
        }
        val keepAlive = HttpUtil.isKeepAlive(req)
        val response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(HttpHelloWorldServerHandler.CONTENT))
        response.headers.set(CONTENT_TYPE, "text/plain")
        response.headers.set(CACHE_CONTROL, "no-cache, no-store, must-revalidate")
        response.headers.set(PRAGMA, "no-cache")
        response.headers.set(CONTENT_LENGTH, response.content.readableBytes)
        response.headers.set(EXPIRES, 0)
        if (!keepAlive) {
          ctx.write(response).addListener(ChannelFutureListener.CLOSE)
        }else {
          response.headers.set(CONNECTION, HttpHeaderValues.KEEP_ALIVE)
          ctx.write(response)
        }
      case _ =>

    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close
  }
}