import Dependencies._

name := "cloudberry"

lazy val web = (project in file("web")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= webDependencies
  ).enablePlugins(PlayScala)

lazy val feed = (project in file("feed")).
  settings(Commons.settings: _*).
  settings(
    libraryDependencies ++= feedDependencies
  )
