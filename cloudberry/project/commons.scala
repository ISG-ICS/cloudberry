import sbt._
import Keys._
import play.sbt.routes.RoutesKeys._
import sbtassembly._
import AssemblyKeys._


object Commons {
  val appVersion = "1.0-SNAPSHOT"
  val appScalaVersion = "2.11.7"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "edu.uci.ics",
    version := appVersion,
    scalaVersion := appScalaVersion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= Seq(
      Opts.resolver.mavenLocalFile,
      "ossrh" at "https://oss.sonatype.org/content/repositories/releases"
    )
  )

  val playSettings = settings ++ Seq(
    routesGenerator := InjectedRoutesGenerator,
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
  )
}
