name := "goat"

version := "7.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion in ThisBuild := "2.10.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "latest.integration",
  "net.sourceforge.javacsv" % "javacsv" % "latest.integration",
  "org.reflections" % "reflections" % "latest.integration",
  "org.twitter4j" % "twitter4j-core" % "latest.integration",
  "org.twitter4j" % "twitter4j-stream" % "latest.integration",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "latest.integration",
  "org.json" % "json" % "latest.integration"
)
