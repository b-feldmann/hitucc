import scala.sys.process._

val akkaVersion = "2.5.24"
val author = "bfeldmann"

enablePlugins(TestNGPlugin)

// to be able to use exit(int) I fork the jvm and kill the fork and not the sbt-shell
fork in run := true

// set the main class for sbt - is not necessary if only using one class with a main method
mainClass := Some("hit_ucc.HitUCCApp")

maintainer := "Benjamin Feldmann <benjamin-feldmann@web.de>"

lazy val app = (project in file("."))
  .settings(
    name := "HitUCC",
    libraryDependencies ++= Seq(
      // -- Logging --
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3",
      // -- Akka --
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      // -- Tests --
      "org.testng" % "testng" % "6.14.2",
      // -- other stuff --
      "org.apache.commons" % "commons-collections4" % "4.0",
      "com.beust" % "jcommander" % "1.72",
      "com.twitter" %% "chill-akka" % "0.9.3",
      "org.projectlombok" % "lombok" % "1.18.2",
      "org.apache.commons" % "commons-collections4" % "4.0",
      "com.opencsv" % "opencsv" % "3.3",
    ),
  )

// define tasks
TaskKey[Unit]("bridgesTask") := (run in Compile).toTask(" peer-host --workers 4 -ddf 8 -i bridges.csv --csvDelimiter ,").value
TaskKey[Unit]("bridgesTaskManyWorker") := (run in Compile).toTask(" peer-host --workers 36 -ddf 8 -i bridges.csv --csvDelimiter ,").value
TaskKey[Unit]("flightTask") := (run in Compile).toTask(" flight peer host system").value
TaskKey[Unit]("ncvoterTask") := (run in Compile).toTask(" peer-host --workers 4 -ddf 3 -i ncvoter_Statewide.10000r.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("ncvoterTaskSingleWorker") := (run in Compile).toTask(" peer-host --workers 1 -ddf 3 -i ncvoter_Statewide.10000r.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("ncvoterPeerTask") := (run in Compile).toTask(" peer --workers 4 --masterhost 169.254.94.1").value
TaskKey[Unit]("chessTask") := (run in Compile).toTask(" peer-host --workers 6 -i chess.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("chessTaskSingle") := (run in Compile).toTask(" peer-host --workers 1 -i chess.csv --csvDelimiter , --csvSkipHeader").value

lazy val afterDockerBuild = taskKey[Unit]("Push Docker to registry")
afterDockerBuild := ({
  val s: TaskStreams = streams.value
  val shell: Seq[String] = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
  val imageName = author + "/" + packageName.value + ":0.1.0-SNAPSHOT"
  val registry = "registry.fsoc.hpi.uni-potsdam.de/" + author + "/" + packageName.value + ":1"
  val dockerTag: Seq[String] = shell :+ "docker tag " + imageName + " " + registry
  val dockerPush: Seq[String] = shell :+ "docker push " + registry
  s.log.info("pushing docker image [" + imageName + "] to registry [" + registry + "]...")
  if((dockerTag #&& dockerPush !) == 0) {
    s.log.success("successfully pushed image to registry!")
  } else {
    throw new IllegalStateException("docker push to registry failed!")
  }
})

// automatically execute 'afterDockerBuild' after docker:publishLocal
publishLocal in Docker := Def.taskDyn {
  val result = (publishLocal in Docker).value
  Def.task {
    val _ = afterDockerBuild.value
    result
  }
}.value

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

// change the name of the project adding the prefix of the user
packageName in Docker := "bfeldmann/" +  packageName.value
//the base docker images
dockerBaseImage := "java:8-jre"