import sbt._
import Keys._

object GoatBuild extends Build {

  // project root

  lazy val root = Project(id = "goat",
                          base = file(".")) dependsOn(dice, goojax, uno, eliza, jcalc)


  // Our own subprojects; things we've (mostly) written ourselves

  lazy val dice = Project(id = "dice",
                            base = file("subprojects/dice"))

  lazy val goojax = Project(id = "goojax",
                            base = file("subprojects/goojax"))

  lazy val uno = Project(id = "uno",
                            base = file("subprojects/uno"))


  // External subprojects; libraries which have neither maven repo nor jar,
  //   or to which we've made minor source alterations

  lazy val eliza = Project(id = "eliza",
                           base = file("vendor/eliza"))

  lazy val jcalc = Project(id = "jcalc",
                           base = file("vendor/jcalc"))
}
