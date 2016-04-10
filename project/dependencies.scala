import sbt._
import Keys._
import play.sbt.PlayImport._

object Dependencies {

  val webDependencies: Seq[ModuleID] = Seq(
    ws, // Play's web services module
    specs2 % Test,
    "org.specs2" %% "specs2-matcher-extra" % "3.6" % Test,
    "org.easytesting" % "fest-assert" % "1.4" % Test,
    "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % Test,
    "org.webjars" % "bootstrap" % "3.3.6",
    "org.webjars" % "flot" % "0.8.0",
    "org.webjars" % "angularjs" % "1.5.0",
    "org.webjars" % "leaflet" % "0.7.7",
    "com.vividsolutions" % "jts" % "1.13",
    "org.wololo" % "jts2geojson" % "0.7.0",
    "com.github.nscala-time" %% "nscala-time" % "2.10.0",
    "org.scalactic" %% "scalactic" % "2.2.6",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test")

  val feedDependencies: Seq[ModuleID] = Seq(
    "com.vividsolutions" % "jts" % "1.13",
    "org.wololo" % "jts2geojson" % "0.7.0")
}
