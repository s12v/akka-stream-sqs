name := "reactive-sqs"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion = "2.4.10"
  val awsVersion = "1.11.33"

  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

    "com.amazonaws" % "aws-java-sdk-sqs" % awsVersion,

    "org.scalatest" %% "scalatest" % "3.0.0" % Test,
    "org.mockito" % "mockito-core" % "2.1.0-RC.1" % Test
  )
}
