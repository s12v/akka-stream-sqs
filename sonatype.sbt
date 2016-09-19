
sonatypeProfileName := "me.snov"

pomExtra in Global := {
  <url>https://github.com/s12v/akka-stream-sqs</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://github.com/s12v/akka-stream-sqs/LICENSE</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:s12v/akka-stream-sqs.git</url>
      <connection>scm:git:git@github.com:s12v/akka-stream-sqs.git</connection>
    </scm>
    <developers>
      <developer>
        <id>s12v</id>
        <name>Sergey Novikov</name>
        <url>https://github.com/s12v</url>
      </developer>
    </developers>
}


// sbt-pgp settings

useGpg := true
//usePgpKeyHex("...")
