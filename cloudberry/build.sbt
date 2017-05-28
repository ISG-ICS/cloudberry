import Dependencies._

name := "cloudberry"

lazy val zion = (project in file("zion")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= zionDependencies
  )

lazy val neo = (project in file("neo")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= neoDependencies
  ).
  enablePlugins(PlayScala).
  dependsOn(zion % "test->test;compile->compile")
