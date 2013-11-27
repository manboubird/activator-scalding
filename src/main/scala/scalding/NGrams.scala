/*
This script was adapted from the tutorial/Tutorial[2-4].scala scripts that
come with the Scalding distribution, which is subject to the Apache License V2.
*/

import com.twitter.scalding._

/**
 * NGrams
 *
 * This example uses the KJV Bible text to demonstrate how compute 
 * NGrams (see http://en.wikipedia.org/wiki/N-gram), including context NGrams,
 * where some of the words are fixed, 
 * such as a prefix string 
 * You invoke the script inside sbt like this:
 *
 *   scalding NGrams --count 20 --ngrams "I love % %" --input data/kjvdat.txt --output output/kjv-ngrams.txt
 *
 * The "ngrams" phrase allows optional prefixes, like the "I love" shown here
 * and "%" placeholders for each additional word that should be matched. 
 * In this example, 4-grams starting with "I love" will be found. One or more "%"
 * can appear anywhere in the string and all whitespace will be replaced a 
 * regular expression to match any whitespace.
 * NOTE: The string is used as a regular expression, so additional regular expression
 * constructs can be used, e.g., "loves?" will match "love" and "loves".
 * The "count" flag means "show the top i most frequent matching ngrams", 
 * where i defaults to 20.
 * Try different ngram phrases and values of count. Try different data sources.
 * This example also uses the debug pipe to dump output to the console. In this
 * case, you'll see the same output that gets written to the output file, which
 * is the list of the ngrams and their frequencies, sorted by frequency descending.
 */

class NGrams(args : Args) extends Job(args) {
  
  val ngramsArg = args.list("ngrams").mkString(" ").toLowerCase
  // To build the RE, replace all "%" with a word matcher and all spaces 
  // in the input expression with a more general whitespace matcher. 
  val ngramsRE = ngramsArg.trim
    .replaceAll("%", """ (\\p{Alnum}+) """)
    .replaceAll("""\s+""", """\\p{Space}+""").r
  val numberOfNGrams = args.getOrElse("count", "20").toInt

  // Used to sort (phrase,count) pairs by the count, descending.
  val countReverseComparator = 
    (tuple1:(String,Int), tuple2:(String,Int)) => tuple1._2 > tuple2._2
      
  // This flow adds a debug step, which writes the records to the console.
  // Note that we also need to discard the original input fields, 'offset, and 'line.
  val lines = TextLine(args("input"))
    .read
    .flatMap('line -> 'ngram) { 
      text: String => ngramsRE.findAllIn(text.trim.toLowerCase).toIterable
    }
    .discard('offset, 'line)
    .groupBy('ngram) { _.size('count) }
    .groupAll { 
      _.sortWithTake[(String,Int)](('ngram,'count) -> 'sorted_ngrams, numberOfNGrams)(countReverseComparator)
    }
    .debug
    .write(Tsv(args("output")))
}

/**
 * Run this example, using default arguments if none are specified.
 */
object NGrams {
  val name = "NGrams"
  val message = "Find all the 4-grams in the King James Version (KJV) of the bible of the form 'x love x x'."

  def main(args: Array[String]) {
    if (args.length != 0) {
      Run.run(name, message, args)
    } else {
      Run.run(name, message,
        Array("--local", "--count", "5", "--ngrams", "% love % %", 
          "--input", "data/kjvdat.txt", 
          "--output", "output/kjv-ngrams.txt"))

      Run.printSomeOutput("output/kjv-ngrams.txt")
    }
  }
}
