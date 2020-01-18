import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
}

object Zio {

  lazy val `zio-streams` = "dev.zio" %% "zio-streams" % "1.0.0-RC17"

}