organization := "me.snov"

name := "akka-stream-sqs"

version := "0.1.2"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Ywarn-unused-import"
)

libraryDependencies ++= {
  val akkaVersion = "2.4.16"
  val akkaHttpVersion = "10.0.3"
  val awsVersion = "1.11.86"

  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.amazonaws" % "aws-java-sdk-sqs" % awsVersion,

    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.mockito" % "mockito-core" % "2.5.6" % Test
  )
}
