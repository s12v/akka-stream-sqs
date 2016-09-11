name := "reactive-sqs"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion = "2.4.10"

  Seq(
//    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,

    "com.amazonaws" % "aws-java-sdk" % "1.11.33",

    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.mockito" % "mockito-core" % "2.1.0-RC.1" % "test"
  )
}
