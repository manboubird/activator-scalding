
import com.twitter.scalding._

import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration
import scala.io.Source

/**
 * This main is intended for use only for the Activator run command as 
 * the default. If you pass no arguments, it runs all of the examples
 * using default arguments. Use the sbt command "scalding" to run any
 * of the examples with the ability to specify your own arguments.
 * See also the companion objects' main methods.
 */

object RunAll {
	def main(args: Array[String]) {
    if (args.length == 0) {
      NGrams.main(args)
      WordCount.main(args)
      FilterUniqueCountLimit.main(args)
      TfIdf.main(args)
    } else {
      Run.run(args(0), "", args)
    }
  }
}

object Run {
  def run(name: String, message: String, args: Array[String]) = {
    println(s"\n==== $name " + ("===" * 20))
    println(message)
    val argsWithName = name +: args
    println(s"Running: ${argsWithName.mkString(" ")}")
		ToolRunner.run(new Configuration, new Tool, argsWithName)
  }

  def printSomeOutput(outputFileName: String, message: String = "") = {
    if (message.length > 0) println(message)
    println("Output in $outputFileName:")
    Source.fromFile(outputFileName).getLines.take(10) foreach println
    println("...\n")
  }
}
