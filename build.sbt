ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"


libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
  "org.jsoup" % "jsoup" % "1.15.4",
  "com.typesafe.play" %% "play-json" % "2.9.4"
)