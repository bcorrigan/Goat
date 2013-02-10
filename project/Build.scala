import sbt._
import Keys._

object GoatBuild extends Build {

  // project root

  lazy val root = Project(id = "goat",
                          base = file(".")) dependsOn(dice, goojax, uno, eliza)


  // Our own subprojects; things we've (mostly) written ourselves

  lazy val dice = Project(id = "dice",
                            base = file("subprojects/dice"))

  lazy val goojax = Project(id = "goojax",
                            base = file("subprojects/goojax"))

  lazy val uno = Project(id = "uno",
                            base = file("subprojects/uno"))


  // External subprojects; things that have no maven repo and no jar,
  //   or that we've made minor alterations to

  lazy val eliza = Project(id = "eliza",
                           base = file("vendor/eliza"))

}
