package example.nettypure

import java.net.SocketAddress
import java.util.Timer
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import com.typesafe.scalalogging.StrictLogging
import example.{Shared, TestClient}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelDuplexHandler, ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelPromise}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory

import scala.io.StdIn

object NettyTest extends App with StrictLogging{

  private final val requestCounter = new AtomicInteger()

  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  var shutdownHook = () => ()

  val scheduler = Executors.newScheduledThreadPool(0)
  scheduler.scheduleAtFixedRate(() => logStats(), 5, 5, TimeUnit.SECONDS)

  // Configure the server.
  val bossGroup, workerGroup = new NioEventLoopGroup()
  try {
    val b = new ServerBootstrap()
    b.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      //.handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new HttpSimpleServerInitializer(requestCounter))

    val ch = b.bind(Shared.port).sync().channel()
    //ch.closeFuture().sync()

    logger.info(s"Server online at http://${Shared.host}:${Shared.port}/\nPress RETURN to stop...")

    //shutdownHook = TestClient.localClient()

    StdIn.readLine() // let it run until user presses return
  } finally {
    scheduler.shutdownNow()
    shutdownHook()
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

  def logStats(): Unit = {
    logger.info(s"request.count: ${requestCounter.get()}")
  }
}

