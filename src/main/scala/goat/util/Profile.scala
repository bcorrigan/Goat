package goat.util

object Profile {
    def timed[T](report: Long=>Unit)(body: =>T) = {
        val start = System.nanoTime
        val r = body
        report(System.nanoTime - start)
        r
    }
 
    private val timeUnits = List("ns", "us", "ms", "s")
    def formatTime(delta:Long) = {
        def formatTime(v:Long, units:List[String], tail:List[String]):List[String] = {
            def makeTail(what:Long) = (what + units.head) :: tail
            if(!units.tail.isEmpty && v >= 1000)
                formatTime(v / 1000, units.tail, makeTail(v % 1000))
            else
                makeTail(v)
        }
        formatTime(delta, timeUnits, Nil).mkString(" ")
    }
    def printTime(msg:String) = (delta:Long) => {
        println(msg + formatTime(delta))
    }
}
