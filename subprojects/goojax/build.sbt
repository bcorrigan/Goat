name := "goojax"

version := "1.2"

libraryDependencies += "com.google.code.gson" % "gson" % "latest.release"

// sbt support for jQuery tests
libraryDependencies +=
  "com.novocode" % "junit-interface" % "latest.integration" % "test->default"
