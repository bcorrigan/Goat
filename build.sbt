name := "goat"

version := "7.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion := "2.10.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"

libraryDependencies += "net.sourceforge.javacsv" % "javacsv" % "latest.integration"

libraryDependencies += "org.reflections" % "reflections" % "latest.integration"