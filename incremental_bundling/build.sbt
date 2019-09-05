import Dependencies._
name := """graph-viz"""
organization := "edu.uci.ics"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

libraryDependencies += guice
libraryDependencies += javaJdbc
libraryDependencies += "org.postgresql" % "postgresql" % "42.1.4"
libraryDependencies ++= graphVizDependencies