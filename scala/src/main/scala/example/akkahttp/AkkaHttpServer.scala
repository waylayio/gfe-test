package example.akkahttp

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpCharsets, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ContentTypes._
import akka.stream.ActorMaterializer
import akka.pattern.after
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import example.Shared
import kamon.metric.PeriodSnapshot
import kamon.{Kamon, MetricReporter}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Random

object AkkaHttpServer extends App with StrictLogging{
  
  final val total = new AtomicInteger()

  Kamon.addReporter(new MetricReporter {

    override def start(): Unit = ()

    override def stop(): Unit = ()

    override def reconfigure(config: Config): Unit = {}

    override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
      snapshot.metrics.rangeSamplers.filter(_.name.startsWith("akka.http.server")).foreach{ s =>
        logger.info(s"${s.name} ${s.distribution.max}")
      }
      logger.info(s"akka.http.total.requests ${total.get()}")
      logger.info(s"openFileDescriptorCount: ${Shared.getOpenFiles()}")
    }
  })


  private implicit val system = ActorSystem("server")
  private implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  private implicit val executionContext = system.dispatcher

  private val route =
    pathSingleSlash {
      complete{
        total.incrementAndGet()
        "Hi from akka\n"
      }
    } ~
      path("slow"){
        parameters('delay.?) { delay =>
          val delayMillis = delay.map(_.toInt).getOrElse(Random.nextInt(1000))
          onComplete(after(delayMillis.millis, system.scheduler)(Future.successful(()))){ _ =>
            complete{
              total.incrementAndGet()
              s"Hi from akka in $delayMillis ms\n"
            }
          }
        }
      } ~ path("big"){
        complete{
          total.incrementAndGet()
          val contentType = `text/plain(UTF-8)`
          val byteStringSource = Source
            .repeat(ByteString("Hi from akka.", contentType.charset.nioCharset()))
            .take(1024 * Random.nextInt(1024))
          HttpEntity(contentType, byteStringSource)
        }
      }

  val bindingFuture = Http().bindAndHandle(route, Shared.host, Shared.port)

  logger.info(s"Server online at http://${Shared.host}:${Shared.port}/\nPress RETURN to stop...")

  //val shutdownHook = TestClient.localClient()

  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete{_ =>
      //shutdownHook()
      Kamon.stopAllReporters()
      system.terminate()
    } // and shutdown when done

}
