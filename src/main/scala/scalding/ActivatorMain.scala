
import com.twitter.scalding._

import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration

/**
 * This main is intended for use only for the Activator run command.
 * If you pass no arguments, it demonstrates one of the examples, NGrams.
 * Use the sbt command "scalding" to run any of the examples and 
 * to vary the arguments passed to it.
 */

object ActivatorMain {
	def main(args: Array[String]) {
    val actualArgs = 
      if (args.length == 0) {
        println("Finding all the 4-grams in the King James Version of the bible")
        println("of the form 'x love x x'.")
        Array(
          "NGrams", "--local", "--count", "5", "--ngrams", "% love % %", 
          "--input", "data/kjvdat.txt", "--output", "output/kjt-ngrams.txt")
      } else {
        args
      }

		ToolRunner.run(new Configuration, new Tool, actualArgs)
	}
}
