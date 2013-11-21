
import com.twitter.scalding._

import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration
import scala.io.Source

/**
 * This main is intended for use only for the Activator run command.
 * If you pass no arguments, it demonstrates one of the examples, NGrams.
 * Use the sbt command "scalding" to run any of the examples and 
 * to vary the arguments passed to it.
 */

object ActivatorMain {
	def main(args: Array[String]) {
    if (args.length == 0) {
      run("NGrams",
        "Find all the 4-grams in the King James Version (KJV) of the bible of the form 'x love x x'.",
        Array(
          "NGrams", "--local", "--count", "5", "--ngrams", "% love % %", 
          "--input", "data/kjvdat.txt", "--output", "output/kjv-ngrams.txt"))

      run("WordCount",
        "Find and count all the words in the KJV.",
        Array(
          "WordCount", "--local", "--input", "data/kjvdat.txt", "--output", "output/kjv-wordcount.txt"))
      // Dump the first 10 lines of the output
      Source.fromFile("output/kjv-wordcount.txt").getLines.take(10) foreach println
      println("...\n")

      run("FilterUniqueCountLimit",
        "Demonstrate filtering records, finding unique records, counting, and limiting output.",
        Array(
          "FilterUniqueCountLimit", "--local", "--input", "data/kjvdat.txt", "--output", "output/kjv"))
      println("Verses filtered to remove miracles (a skeptics KJV...):")
      Source.fromFile("output/kjv-skeptic.txt").getLines.take(10) foreach println
      println("...\n")
      println("Unique list of books of the KJV:")
      Source.fromFile("output/kjv-books.txt").getLines foreach println
      println("\n")
      println("Number of verses in the KJV:")
      Source.fromFile("output/kjv-count-star.txt").getLines foreach println
      println("...")
      println("The first 10 lines:")
      Source.fromFile("output/kjv-limit-N.txt").getLines foreach println
      println("\n")

      run("TfIdf",
        "Compute term frequency-inverse document frequency on the KJV.",
        Array(
          "TfIdf", "--local", "--input", "data/kjvdat.txt", "--output", "output/kjv-tfidf.txt", "--n", "100"))
      Source.fromFile("output/kjv-tfidf.txt").getLines.take(10) foreach println
      println("...")
    } else {
      run(args(0), "", args)
    }
  }

  def run(name: String, message: String, args: Array[String]) = {
    println(s"\n==== $name " + ("===" * 20))
    println(message)
    println(s"Running: ${args.mkString(" ")}")
		ToolRunner.run(new Configuration, new Tool, args)
  }
}
