name := "goat"

version := "7.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion in ThisBuild := "2.10.0"

libraryDependencies ++= Seq(

  // things that need to be locked to a specific version.  Avoid this if possible.
  "commons-lang" % "commons-lang" % "2.6",
  "org.python" % "jython" % "2.5.3",

  // normal libs, use the latest release
  "com.typesafe.akka" %% "akka-actor" % "latest.integration",
  "net.sourceforge.javacsv" % "javacsv" % "latest.integration",
  "org.reflections" % "reflections" % "latest.integration",
  "org.twitter4j" % "twitter4j-core" % "latest.integration",
  "org.twitter4j" % "twitter4j-stream" % "latest.integration",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "latest.integration",
  "com.sleepycat" % "je" % "latest.integration",

  // and of course, every json lib in the world:
  "org.json" % "json" % "latest.integration",
  "com.fasterxml.jackson.core" % "jackson-databind" % "latest.integration"
)

// Add sbt support for jQuery tests
libraryDependencies +=
  "com.novocode" % "junit-interface" % "latest.integration" % "test->default"
