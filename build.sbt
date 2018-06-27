import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "gfetest",
    fork in run := true,
    connectInput := true,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",

      // akka http
      "com.typesafe.akka" %% "akka-http" % "10.1.3",
      "com.typesafe.akka" %% "akka-actor" % "2.5.13",
      "com.typesafe.akka" %% "akka-stream" % "2.5.13",
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.13",
      "io.kamon" %% "kamon-core" % "1.1.2",
      "io.kamon" %% "kamon-akka-http-2.5" % "1.1.0",

      // jersey
      "org.glassfish.jersey.containers" % "jersey-container-grizzly2-http" % "2.27",
      "org.glassfish.jersey.inject" % "jersey-hk2" % "2.26",
      "org.slf4j" % "jul-to-slf4j" % "1.7.25",
      "org.glassfish.grizzly" % "grizzly-framework-monitoring" % "2.4.3",
      "org.glassfish.grizzly" % "grizzly-http-monitoring" % "2.4.3",
      "org.glassfish.grizzly" % "grizzly-http-server-monitoring" % "2.4.3",

      scalaTest % Test
    )

  )

// https://github.com/sbt/sbt/issues/3618
val workaround = {
  sys.props += "packaging.type" -> "jar"
  ()
}
