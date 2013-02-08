name := "goat"

version := "7.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion := "2.10.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "latest.integration"

libraryDependencies += "net.sourceforge.javacsv" % "javacsv" % "latest.integration"

libraryDependencies += "org.reflections" % "reflections" % "latest.integration"

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "latest.integration"

libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "latest.integration"
