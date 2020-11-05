import scala.sys.process._

name := "goat"

version := "4.0"

mainClass in (Compile, run) := Some("goat.Goat")

scalaVersion in ThisBuild := "2.13.3"

resolvers += Resolver.bintrayIvyRepo("com.eed3si9n", "sbt-plugins")


// make pythons work plz
val pyLibs=List("vendor/libpy","src/main/python")

pyLibs map { pyLib =>
  javaOptions += "-Dpython.path=" + ((baseDirectory) map { bd => Attributed.blank(bd / pyLib) }).toString
  Runtime / unmanagedClasspath += baseDirectory.value / "pylib"
}

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
  "org.python" % "jython-standalone" % "2.5.+", // picks up 2.5 betas, but not 2.7
  "org.mapdb" % "mapdb" % "0.9.7"
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
  "net.sourceforge.javacsv" % "javacsv" % "2.0",
  "org.eclipse.mylyn.github" % "org.eclipse.egit.github.core" % "latest.integration",
  "org.json" % "json" % "latest.integration",
  "org.reflections" % "reflections" % "latest.integration",
  "org.twitter4j" % "twitter4j-core" % "latest.integration",
  "org.twitter4j" % "twitter4j-stream" % "latest.integration",
  "biz.source_code" % "base64coder" % "latest.integration",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "latest.integration"
)


// sbt support for jQuery tests
libraryDependencies +=
  "com.novocode" % "junit-interface" % "latest.integration" % "test->default"


// make version and name available at runtime via sbt-buildinfo plugin;
// they will be available in class goat.Buildinfo (via .name(), .version(), etc)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, BuildInfoKey.action("gitRevision") {
      try {
        ("git log --no-merges --oneline" lineStream_!).length.toString
      } catch {
        case ioe: java.io.IOException => "???"
      }}),
    buildInfoPackage := "goat"
  )

//EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed

// project root
lazy val goatRoot = (project in file("."))
  .aggregate(dice)
  .aggregate(goojax)
  .aggregate(uno)
  .aggregate(eliza)
  .aggregate(jcalc)
  .settings(
    name := "Goat"
  )

// Our own subprojects; things we've (mostly) written ourselves
lazy val dice = (project in file("subprojects/dice"))
  .settings(
    name := "dice"
  )

lazy val goojax = (project in file("subprojects/goojax"))
  .settings(
    name := "goojax"
  )

lazy val uno = (project in file("subprojects/uno"))
  .settings(
    name := "uno"
  )

// External subprojects; libraries which have neither maven repo nor jar,
//   or to which we've made minor source alterations
lazy val eliza = (project in file("vendor/eliza"))
  .settings(
    name := "eliza"
  )

lazy val jcalc = (project in file("vendor/jcalc"))
  .settings(
    name := "jcalc"
  )

