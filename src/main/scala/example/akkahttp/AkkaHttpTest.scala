package example.akkahttp

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import example.Shared
import kamon.metric.PeriodSnapshot
import kamon.{Kamon, MetricReporter}

import scala.io.StdIn

object AkkaHttpTest extends App with StrictLogging{
  
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
    }
  })


  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val route =
    pathSingleSlash {
      complete{
        total.incrementAndGet()
        "Hi from akka"
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
