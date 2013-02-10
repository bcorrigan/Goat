import sbt._
import Keys._

object GoatBuild extends Build {
  lazy val root = Project(id = "goat",
                          base = file(".")) dependsOn(goojax, eliza)

  lazy val goojax = Project(id = "goojax",
                            base = file("subprojects/goojax"))

  lazy val eliza = Project(id = "eliza",
                           base = file("vendor/eliza"))

}
