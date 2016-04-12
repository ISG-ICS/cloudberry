import Dependencies._

name := "cloudberry"

lazy val util = (project in file("util")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= utilDependencies
  )

lazy val oracle = (project in file("oracle")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= oracleDependencies
  ).dependsOn(util)

lazy val noah = (project in file("noah")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= noahDependencies
  ).dependsOn(util)

lazy val neo = (project in file("neo")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= neoDependencies
  ).
  enablePlugins(PlayScala).
  dependsOn(oracle, util)

