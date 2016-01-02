name := "u2f-scala-example"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.camel" % "camel-core" % "2.16.0",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M1",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M1",
  "redis.clients" % "jedis" % "2.8.0",
  "org.spire-math" %% "cats" % "0.3.0",
  "com.yubico" % "u2flib-server-core" % "0.15.0")

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)
