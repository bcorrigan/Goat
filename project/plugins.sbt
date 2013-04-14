// generate .ensime file with 'ensime generate' at the sbt prompt
addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.0")

// used to make project name and version available at runtime; see build.sbt
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.2")