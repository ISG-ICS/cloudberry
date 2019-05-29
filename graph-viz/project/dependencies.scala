import sbt._
import play.sbt.PlayImport._

object Dependencies {
  val playVersion = "2.5.0"
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

  val graphVizDependencies: Seq[ModuleID] = Seq(
    ws, // Play's web services module
    "org.webjars" % "bootstrap" % "3.3.6",
    "org.webjars" % "angularjs" % "1.5.0",
    // map module
    "org.webjars.bower" % "json-bigint" % "0.0.0",
    "org.webjars.bower" % "bootstrap-toggle" % "2.2.2",
    // mapresultcache module
    "org.webjars.bower" % "hashmap" % "2.0.5",
    // timeseries module
    "org.webjars.bower" % "crossfilter" % "1.3.11",
    "org.webjars.bower" % "dc.js" % "1.7.5",
    "org.webjars" % "d3js" % "3.5.16"
  ) ++ testDeps
}
