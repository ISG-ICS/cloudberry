import sbt._
import play.sbt.PlayImport._

object Dependencies {
  val playVersion = "2.5.0"
  val twitter4jVersion = "4.0.3"
  val mockitoAll = "org.mockito" % "mockito-all" % "1.10.19" % Test
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.6" % Test
  val easytest = "org.easytesting" % "fest-assert" % "1.4" % Test
  val testDeps = Seq(
    specs2 % Test,
    "org.specs2" %% "specs2-matcher-extra" % "3.6" % Test,
    "org.easytesting" % "fest-assert" % "1.4" % Test,
    "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % Test,
    scalatest,
    easytest,
    mockitoAll
  )

  val neoDependencies: Seq[ModuleID] = Seq(
    ws, // Play's web services module
    "org.webjars" % "bootstrap" % "3.3.6"
    // other module
  ) ++ testDeps

  val zionDependencies: Seq[ModuleID] = Seq(
    "joda-time" % "joda-time" % "2.9.3",
    "com.typesafe.akka" %% "akka-actor" % "2.4.4",
    "org.apache.spark" % "spark-core_2.11" % "2.1.0",
    "org.apache.spark" % "spark-sql_2.11" % "2.1.0",
    "org.scalikejdbc" %% "scalikejdbc" % "2.5.2",
    "mysql" % "mysql-connector-java" % "5.1.24",
    "com.typesafe.play" %% "anorm" % "2.5.1",
    ("com.typesafe.play" %% "play-json" % playVersion).exclude("commons-logging", "commons-logging"),
    "com.typesafe.play" %% "play-ws" % playVersion
  ) ++ testDeps
}
