import Dependencies._

name := "twittermap"

lazy val twittermap = (project in file(".")).aggregate(util, gnosis, noah, web)

lazy val web = (project in file("web")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= twittermapDependencies
  ).
  settings(
    mappings in Universal ++=
      (baseDirectory.value / "public" / "data" * "*" get) map
        (x => x -> ("public/data/" + x.getName))
  ).
  enablePlugins(PlayScala)

lazy val util = (project in file("util")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= utilDependencies
  )

lazy val gnosis = (project in file("gnosis")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= gnosisDependencies
  ).dependsOn(util)

lazy val noah = (project in file("noah")).
  settings(Commons.settings: _*).
  settings(
    //    mainClass := Somedd("edu.uci.ics.cloudberry.noah.TwitterJSONTagToADM"),
    libraryDependencies ++= noahDependencies
  ).dependsOn(gnosis, util)
