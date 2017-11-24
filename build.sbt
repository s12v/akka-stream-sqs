organization := "me.snov"

name := "akka-stream-sqs"

version := "0.2.1"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

libraryDependencies ++= {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.3"
  val awsVersion = "1.11.103"

  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.amazonaws" % "aws-java-sdk-sqs" % awsVersion,

    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.mockito" % "mockito-core" % "2.7.16" % Test
  )
}
