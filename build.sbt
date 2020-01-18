import Dependencies._
import Zio._

ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.kpritam"
ThisBuild / organizationName := "com/kpritam"

lazy val root = (project in file("."))
  .settings(
    name := "zio-streams",
    libraryDependencies ++= Seq ( 
      `zio-streams`,
      scalaTest % Test 
      )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
