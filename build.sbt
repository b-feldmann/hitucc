import scala.sys.process._

val akkaVersion = "2.5.26"
val author = "bfeldmann"

enablePlugins(TestNGPlugin)

// to be able to use exit(int) I fork the jvm and kill the fork and not the sbt-shell
fork in run := true
javaOptions in run += "-Xmx4096M"

// set the main class for sbt - is not necessary if only using one class with a main method
mainClass := Some("hitucc.HitUCCApp")

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
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      // -- Tests --
      "org.testng" % "testng" % "6.14.2",
      // -- other stuff --
      "org.apache.commons" % "commons-collections4" % "4.0",
      "com.beust" % "jcommander" % "1.72",
      "com.twitter" %% "chill-akka" % "0.9.4",
      "org.projectlombok" % "lombok" % "1.18.10" % "provided",
      "org.apache.commons" % "commons-collections4" % "4.0",
      "com.opencsv" % "opencsv" % "3.3",
      "org.roaringbitmap" % "RoaringBitmap" % "0.8.9",
      "org.javolution" % "javolution" % "5.3.1",
      // -- Output --
      "com.googlecode.json-simple" % "json-simple" % "1.1.1"
    )
  )

// define tasks
TaskKey[Unit]("bridgesTask") := (run in Compile).toTask(" host --workers 4 -ddf 8 -i bridges.csv --csvDelimiter ,").value
TaskKey[Unit]("bridgesTaskManyWorker") := (run in Compile).toTask(" host --workers 36 -ddf 8 -i bridges.csv --csvDelimiter ,").value
TaskKey[Unit]("ncvoterTask") := (run in Compile).toTask(" host -i ncvoter_Statewide.10000r.csv -w 1 -ddf 1 --csvDelimiter , --csvSkipHeader --createDiffSets trie").value
TaskKey[Unit]("ncvoterTaskSingleWorker") := (run in Compile).toTask(" host --workers 1 -i ncvoter_Statewide.10000r.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("ncvoterPeerTask") := (run in Compile).toTask(" peer --workers 4 --masterhost 127.17.0.7 --masterport 1600").value
TaskKey[Unit]("chessTask") := (run in Compile).toTask(" host --workers 6 -i cheass.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("chessTaskSingle") := (run in Compile).toTask(" host --workers 1 -i chess.csv --csvDelimiter , --csvSkipHeader").value
TaskKey[Unit]("censusTask") := (run in Compile).toTask(" host --workers 8 -i CENSUS.csv --csvDelimiter ; --csvSkipHeader").value

lazy val afterDockerBuild = taskKey[Unit]("Push Docker to registry")
afterDockerBuild := ({
  val s: TaskStreams = streams.value
  val shell: Seq[String] = if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
  val imageName = author + "/" + packageName.value
  val registry = "registry.fsoc.hpi.uni-potsdam.de/" + author + "/" + packageName.value
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
//publishLocal in Docker := Def.taskDyn {
//  val result = (publishLocal in Docker).value
//  Def.task {
//    val _ = afterDockerBuild.value
//    result
//  }
//}.value

enablePlugins(JavaServerAppPackaging)

// change the name of the project adding the prefix of the user
packageName in Docker := author + "/" + packageName.value

version in Docker := "latest"

dockerExposedPorts := Seq(8000)

dockerBaseImage := "java:8-jre"