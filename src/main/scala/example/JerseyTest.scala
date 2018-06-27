package example

import java.lang.management.ManagementFactory
import java.net.URI
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.StrictLogging
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.slf4j.bridge.SLF4JBridgeHandler

import scala.collection.JavaConverters._
import scala.io.StdIn
import scala.util.Try

object JerseyTest extends App with StrictLogging{

  // Optionally remove existing handlers attached to j.u.l root logger
  SLF4JBridgeHandler.removeHandlersForRootLogger()  // (since SLF4J 1.6.5)

  // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
  // the initialization phase of your application
  SLF4JBridgeHandler.install()

  // Base URI the Grizzly HTTP server will listen on
  val BASE_URI = URI.create(s"http://localhost:${Shared.port}")

  import org.glassfish.jersey.server.ResourceConfig

  val rc = new ResourceConfig()
  val rcw = new ResourceConfigWrapper(rc)
  rcw.register(new MyResource())

//  rcw.register(
//    new LoggingFeature(
//      Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
//      Level.INFO,
//      LoggingFeature.Verbosity.PAYLOAD_ANY,
//      Integer.MAX_VALUE)
//  )


  // create and start a new instance of grizzly http server
  // exposing the Jersey application at BASE_URI
  val server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc)
  server.getServerConfiguration.setJmxEnabled(true)

  private lazy val mbeanServer = ManagementFactory.getPlatformMBeanServer

  val pool = Executors.newScheduledThreadPool(0)
  pool.scheduleAtFixedRate(() => Try{
    val res1 = mbeanServer.queryMBeans(null, null)
    res1.asScala.find(_.toString.contains("TCPNIOTransport")).foreach{ nio =>
      val res = mbeanServer.getMBeanInfo(nio.getObjectName)
      res.getAttributes.find(_.getName == "open-connections-count").foreach(a =>
        logger.info(a.getName + " " + mbeanServer.getAttribute(nio.getObjectName, a.getName))
      )
      res.getAttributes.find(_.getName == "total-connections-count").foreach(a =>
        logger.info(a.getName + " " + mbeanServer.getAttribute(nio.getObjectName, a.getName))
      )
    }
    res1.asScala.find(_.toString.contains("HttpServerFilter")).foreach{ serverFilter =>
      val res = mbeanServer.getMBeanInfo(serverFilter.getObjectName)
      res.getAttributes.find(_.getName == "requests-completed-count").foreach(a =>
        logger.info(a.getName + " " + mbeanServer.getAttribute(serverFilter.getObjectName, a.getName))
      )
    }
    res1.asScala.find(_.toString.contains("Keep-Alive")).foreach{ serverFilter =>
      val res = mbeanServer.getMBeanInfo(serverFilter.getObjectName)
      res.getAttributes.find(_.getName == "live-connections-count").foreach(a =>
        logger.info(a.getName + " " + mbeanServer.getAttribute(serverFilter.getObjectName, a.getName))
      )
    }
  }.recover{
    case e => logger.error("error getting metrics", e)
  }, 0, 5, TimeUnit.SECONDS)


  logger.info(s"Jersey app started at $BASE_URI \nHit enter to stop it...")

  //val shutdownHook = Shared.localClient()

  StdIn.readLine() // let it run until user presses return

  //shutdownHook()
  server.shutdownNow()
  pool.shutdownNow()

}
