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
    libraryDependencies ++= noahDependencies
  ).dependsOn(gnosis, util)

lazy val neo = (project in file("neo")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= neoDependencies
  ).
  enablePlugins(PlayScala).
  dependsOn(gnosis, util)

