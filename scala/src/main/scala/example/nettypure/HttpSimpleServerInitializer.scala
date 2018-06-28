package example.nettypure

import java.util.concurrent.atomic.AtomicInteger

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpContentCompressor, HttpRequestDecoder, HttpResponseEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

class HttpSimpleServerInitializer(requestCounter: AtomicInteger) extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline()
    p
      .addLast("decoder", new HttpRequestDecoder())
      // Uncomment the following line if you don't want to handle HttpChunks.
      //    p.addLast("aggregator", new HttpObjectAggregator(1048576))
      .addLast("encoder", new HttpResponseEncoder())
      // Remove the following line if you don't want automatic content compression.
      .addLast("deflater", new HttpContentCompressor())
      .addLast("handler", new HttpHelloWorldServerHandler(requestCounter))
  }
}
