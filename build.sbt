import Dependencies._

name := "cloudberry"

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
    //    mainClass := Some("edu.uci.ics.cloudberry.noah.TwitterJSONTagToADM"),
    libraryDependencies ++= noahDependencies
  ).dependsOn(gnosis, util)

lazy val zion = (project in file("zion")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= zionDependencies
  ).dependsOn(util)

lazy val neo = (project in file("neo")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= neoDependencies
  ).
  settings(
    mappings in Universal ++=
      (baseDirectory.value / "public" / "data" * "*" get) map
        (x => x -> ("public/data/" + x.getName))
  ).
  enablePlugins(PlayScala).
  dependsOn(gnosis, util, zion % "test->test;compile->compile")
