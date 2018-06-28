package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object TestClient extends StrictLogging{

  def main(args: Array[String]) : Unit = {
    val uri = Uri("https://francistest.waylay.io")
    //val uri = Uri("https://francistest.waylay.io/slow?delay=1000")
    val shutdown = localClient(uri, maxConnections = 512)
    logger.info(s"Generating load on $uri, press return to stop...")
    StdIn.readLine()
    shutdown()
  }

  def localClient(uri: Uri = s"http://localhost:${Shared.port}", maxConnections: Int = 1): () => Unit = {
    implicit val system = ActorSystem("client")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val settings = ConnectionPoolSettings(system)
      .withMaxConnections(maxConnections)
      .withMaxOpenRequests(1024)
    val pool = uri.scheme match {
      case "http" =>
        Http().cachedHostConnectionPool[Unit](uri.authority.host.address(), uri.authority.port, settings)
      case "https" =>
        val port = Some(uri.authority.port).filter(_ != 0).getOrElse(443)
        Http().cachedHostConnectionPoolHttps[Unit](uri.authority.host.address(), port, settings = settings)
    }

    val source = Source
      .repeat(())
      .viaMat(KillSwitches.single)(Keep.right)
      .map { _ =>
        (HttpRequest(uri = uri), ())
      }
      .via(pool)
      .via(printFlowRate[(Try[HttpResponse], Unit)]("responses/sec", _ => 1, 1.seconds))
      .map{
        case (Success(HttpResponse(StatusCodes.OK, _, entity, _)), _) =>
          //logger.info(resp.status.toString())
          entity.discardBytes()
        case (Success(HttpResponse(otherStatus, _, entity, _)), _) =>
          logger.warn(s"Request failed with code $otherStatus")
          entity.discardBytes()
        case (Failure(e), _) => logger.error("Req failed", e)
      }

    val (kill, f) = source
      .toMat(Sink.ignore)(Keep.both)
      .run()

    f.onComplete(println)

    () => {
      kill.shutdown()
      Http().shutdownAllConnectionPools().onComplete{ _ =>
        system.terminate()
      }
    }
  }

  def flowRate[T](metric: T => Int = (_: T) => 1, outputDelay: FiniteDuration = 1.second): Flow[T, Double, NotUsed] = Flow[T]
    .conflateWithSeed(metric(_)){ case (acc, x) ⇒ acc + metric(x) }
    .zip(Source.tick(outputDelay, outputDelay, NotUsed))
    .map(_._1.toDouble / outputDelay.toUnit(SECONDS))

  def printFlowRate[T](name: String, metric: T => Int = (_: T) => 1, outputDelay: FiniteDuration = 1.second): Flow[T, T, NotUsed] =
    Flow[T]
      .alsoTo(flowRate[T](metric, outputDelay)
        .to(Sink.foreach(r => logger.info(s"Rate($name): $r"))))
}
