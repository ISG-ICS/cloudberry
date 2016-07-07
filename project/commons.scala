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
      Resolver.sonatypeRepo("snapshots"),
      Resolver.mavenLocal,
      "ossrh" at "https://oss.sonatype.org/content/repositories/releases"
    ),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp filter((x)=> (x.data.getName=="algebricks-common-0.2.18-SNAPSHOT.jar" ||
        x.data.getName=="hyracks-api-0.2.18-SNAPSHOT.jar" ||
        x.data.getName == "asterix-external-data-0.8.9-SNAPSHOT.jar" ||
        x.data.getName == "asterix-common-0.8.9-SNAPSHOT.jar"))
    },
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
      case "META-INF/DISCLAIMER" => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

  val playSettings = settings ++ Seq(
    routesGenerator := InjectedRoutesGenerator,
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
  )
}
