
sonatypeProfileName := "me.snov"

pomExtra in Global := {
  <url>https://github.com/s12v/akka-stream-sqs</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
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
