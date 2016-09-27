import com.typesafe.sbt.packager.docker.Cmd

name := """actyx-dashboard"""

scalaVersion := "2.11.8"

version := "0.0.2"
val Akka  = "2.4.10"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

includeFilter in (Assets, LessKeys.less) := "main.less"

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  ws,
  "org.webjars"            %  "bootstrap" % "3.3.6",
  "org.webjars"            %  "flot"      % "0.8.3",
  "com.datastax.cassandra" %  "cassandra-driver-core"  %  "3.1.0",
  "com.typesafe.akka"      %% "akka-stream-kafka"   % "0.12",
  "com.typesafe.akka"      %% "akka-cluster"        % Akka,
  "org.scalatestplus.play" %% "scalatestplus-play"  % "1.5.1" % Test
)

routesGenerator := InjectedRoutesGenerator

// --------------------
// ------ DOCKER ------
// --------------------
// build with activator docker:publishLocal

maintainer := "haghard"
dockerRepository := Option("haghard")
dockerBaseImage := "frolvlad/alpine-oraclejdk8:latest"

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("FROM", _) => (cmd :: Cmd("RUN", "apk update && apk add bash && ls -la") :: Nil)
  case otherCmd =>  List(otherCmd)
}

//run -Dhttp.port=9000 -DGMAPS_API_KEY=... -Dakka.cluster.seed=192.168.0.62:2551 -Dakka.remote.netty.tcp.port=2601 -Dakka.remote.netty.tcp.hostname=192.168.0.62