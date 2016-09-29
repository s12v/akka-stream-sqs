organization := "me.snov"

name := "akka-stream-sqs"

version := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion = "2.4.10"
  val awsVersion = "1.11.33"

  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.amazonaws" % "aws-java-sdk-sqs" % awsVersion,

    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "org.mockito" % "mockito-core" % "2.1.0-RC.1" % Test
  )
}
