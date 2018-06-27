package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import example.Shared.{logger, port}

import scala.util.{Failure, Success}

object TestClient extends StrictLogging{

  def localClient(): () => Unit = {
    implicit val system = ActorSystem("client")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val settings = ConnectionPoolSettings(system)
      .withMaxConnections(64)
      .withMaxOpenRequests(1024)
    val pool = Http().cachedHostConnectionPool[Unit]("localhost", port, settings)

    val source = Source
      .repeat(())
      .viaMat(KillSwitches.single)(Keep.right)
      .map { _ =>
        (HttpRequest(uri = s"http://localhost:$port"), ())
      }
      .via(pool)
      .map{
        case (Success(resp), _) =>
          //logger.info(resp.status.toString())
          resp.entity.discardBytes()
        case (Failure(e), _) => logger.error("Req failed", e)
      }

    val (kill, f) = source
      .toMat(Sink.ignore)(Keep.both)
      .run()

    f.onComplete(println)

    () => {
      kill.shutdown()
      system.terminate()
    }
  }
}
