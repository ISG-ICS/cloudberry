import Dependencies._

name := "coronavirustwittermap"

lazy val coronavirustwittermap = (project in file(".")).aggregate(util, web)

lazy val web = (project in file("web")).
  settings(Commons.playSettings: _*).
  settings(
    libraryDependencies ++= coronavirustwittermapDependencies
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
