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
    ),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
      case PathList("org", "apache", "commons", xs @_ *) => MergeStrategy.first
      case "META-INF/io.netty.versions.properties" => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

  val playSettings = settings ++ Seq(
    routesGenerator := InjectedRoutesGenerator,
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  )
}
