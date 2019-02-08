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

  val noahDependencies: Seq[ModuleID] = Seq(
    ws,
    "org.twitter4j" % "twitter4j-stream" % twitter4jVersion,
    "org.twitter4j" % "twitter4j-core" % twitter4jVersion,
    "com.twitter" % "hbc-core" % "2.2.0",
    "org.kohsuke.args4j" % "args4j-maven-plugin" % "2.33",
    "com.crawljax" % "crawljax-core" % "3.6",
    "com.crawljax.plugins" % "crawloverview-plugin" % "3.6",
    "com.crawljax" % "crawljax-cli" % "3.6",
    "org.apache.kafka" % "kafka_2.11" % "0.10.0.0"
      exclude("javax.jms", "jms")
      exclude("com.sun.jdmk", "jmxtools")
      exclude("org.slf4j", "slf4j-log4j12")
      exclude("com.sun.jmx", "jmxri"),
    "org.jsoup" % "jsoup" % "1.9.2",
    "com.buzzilla" % "webhose" % "0.1.1",
    ("org.apache.commons" % "commons-lang3" % "3.4").exclude("commons-logging", "commons-logging")
  ) ++ testDeps

  val utilDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-logback" % playVersion
  ) ++ testDeps

  val gnosisDependencies: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % "2.2.6",
    ("com.typesafe.play" %% "play-json" % playVersion).exclude("commons-logging", "commons-logging"),
    "com.vividsolutions" % "jts" % "1.13",
    "org.wololo" % "jts2geojson" % "0.7.0"
  ) ++ testDeps

  val twittermapDependencies: Seq[ModuleID] = Seq(
    ws, // Play's web services module
    "org.webjars" % "bootstrap" % "3.3.6",
    "org.webjars" % "flot" % "0.8.0",
    "org.webjars" % "angularjs" % "1.5.0",
    "org.eclipse.jetty.websocket" % "websocket-client" % "9.4.7.v20170914",
    // map module
    "org.webjars" % "leaflet" % "0.7.7",
    "org.webjars" % "angular-leaflet-directive" % "0.8.2",
    "org.webjars.bower" % "json-bigint" % "0.0.0",
    "org.webjars.bower" % "bootstrap-toggle" % "2.2.2",
    // mapresultcache module
    "org.webjars.bower" % "hashmap" % "2.0.5",
    // timeseries module
    "org.webjars.bower" % "crossfilter" % "1.3.11",
    "org.webjars.bower" % "dc.js" % "1.7.5",
    "org.webjars" % "d3js" % "3.5.16",
    // sidebar module
    "org.webjars" % "font-awesome" % "4.5.0",
    "org.webjars.bower" % "bootstrap-vertical-tabs" % "1.2.1",
    // Added jquery-ui for showing/hiding the time series histogram.
    "org.webjars.bower" % "jquery-ui" % "1.12.1",
    // draw chart module
    "org.webjars.bower" % "chart.js" % "2.7.2",
    // Added jquery-ui theme for decorating auto-complete menu
    "org.webjars" % "jquery-ui-themes" % "1.12.1",
    // Added twitter4j for twittermap
    "org.twitter4j" % "twitter4j-stream" % twitter4jVersion,
    "org.twitter4j" % "twitter4j-core" % twitter4jVersion

  ) ++ testDeps
}
