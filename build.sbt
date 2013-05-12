name := "goat"

version := "4.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion in ThisBuild := "2.10.0"


// make pythons work plz
javaOptions += "-Dpython.path=" + ((baseDirectory) map { bd => Attributed.blank(bd / "libpy") }).toString

unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "libpy") }


// the trustStore javaOption is not picked up unless we fork
fork := true

// and we use a local trustStore because the one that ships with freebsd java is poop
javaOptions += "-Djavax.net.ssl.trustStore=config/cacerts"


// Dependency madness begins here

// things that need to be locked to a specific version.  Avoid this if possible.
// for help with ivy version specifiers, see:
// http://ant.apache.org/ivy/history/2.2.0/ivyfile/dependency.html#revision
libraryDependencies ++= Seq(
  "commons-lang" % "commons-lang" % "2.+",
  "org.apache.lucene" % "lucene-core" % "2.+",
  "org.python" % "jython-standalone" % "2.5.+" // picks up 2.5 betas, but not 2.7
)

// normal libs, use version latest.integration
// NOTE: to see which versions sbt has chosen, do 'show update' at the sbt prompt
libraryDependencies ++= Seq(
  "de.u-mass" % "lastfm-java" % "latest.integration",
  "com.fasterxml.jackson.core" % "jackson-databind" % "latest.integration",
  "com.omertron" % "rottentomatoesapi" % "latest.integration",
  "com.sleepycat" % "je" % "latest.integration",
  "com.typesafe.akka" %% "akka-actor" % "latest.integration",
  "jivesoftware" % "smackx" % "latest.integration",
  "log4j" % "log4j" % "latest.integration",
  "net.sourceforge.javacsv" % "javacsv" % "latest.integration",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "latest.integration",
  "org.json" % "json" % "latest.integration",
  "org.reflections" % "reflections" % "latest.integration",
  "org.twitter4j" % "twitter4j-core" % "latest.integration",
  "org.twitter4j" % "twitter4j-stream" % "latest.integration",
  "biz.source_code" % "base64coder" % "latest.integration",
  "org.mapdb" % "mapdb" % "latest.integration"
)


// sbt support for jQuery tests
libraryDependencies +=
  "com.novocode" % "junit-interface" % "latest.integration" % "test->default"


// make version and name available at runtime via sbt-buildinfo plugin;
// they will be available in class goat.Buildinfo (via .name(), .version(), etc)
buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  BuildInfoKey.action("gitRevision") {
    try {
      ("git log --no-merges --oneline" lines_!).length.toString
    } catch {
      case ioe: java.io.IOException => "???"
    }})

buildInfoPackage := "goat"
