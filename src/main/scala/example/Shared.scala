package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}

import scala.util.{Failure, Success}

object Shared extends LazyLogging{
  final val port = 9002
  final val host = "0.0.0.0"


}
