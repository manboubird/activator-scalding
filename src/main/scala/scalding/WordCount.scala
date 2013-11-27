/*
This script was adapted from the tutorial/Tutorial[2-4].scala scripts that
come with the Scalding distribution, which is subject to the Apache License v2.
*/

import com.twitter.scalding._

/**
 * WordCount
 *
 * This example implements the (in)famous "hello world!" of Hadoop programming,
 * "Word Count", where a corpus of documents is read, the contents are tokenized
 * into words, and the total count for each word over the entire corpus is computed.
 * You invoke the script inside sbt like this:
 *
 *   scalding WordCount --input data/kjvdat.txt --output output/kjv-wc.txt
 *
 * where the --input and --output arguments specify the location of an input file to
 * be read (a text file of the King James Version of the Bible, in this case) and where
 * to write the output of word-count pairs. Try different values for these arguments.
 * 
 * Exercise: Each line actually has the format: 
 *   book of the Bible (e.g., Genesis) | chapter | verse | text
 * The first three "fields" actually mess up the results a bit. Modify the script to remove
 * these prefixes.
 */
class WordCount(args : Args) extends Job(args) {

  // Tokenize into words by by splitting on non-characters. This is imperfect, as it
  // splits hyphenated words, possessives ("John's"), etc.
  val tokenizerRegex = """\W+"""
  
  // Read the file specified by the --input argument and process each line
  // by trimming the leading and trailing whitespace, converting to lower case,
  // then tokenizing into words.
  // The first argument list to flatMap specifies that we pass the 'line field
  // to the anonymous function on each call and each word in the returned 
  // collection of words is given the name 'word. Note that TextLine automatically
  // associates the name 'line with each line of text. It also tracks the line number
  // and names that field 'offset. Here, we're dropping the offset.
  TextLine(args("input"))
    .read
    .flatMap('line -> 'word) {
      line : String => line.trim.toLowerCase.split(tokenizerRegex) 
    }

  // At this point we have a stream of words in the pipeline. To count 
  // occurrences of the same word, we need to group the words together.
  // The groupBy operation does this. The first argument list to groupBy
  // specifies the fields to group over as the key. In this case, we only
  // use the 'word field. 
  // The anonymous function is passed an object of type com.twitter.scalding.GroupBuilder.
  // All we need to do is compute the size of the group and we give it an 
  // optional name, 'count.
    .groupBy('word){ group => group.size('count) }

  // In many data flows, we would need to project out just the 'word and 'count, 
  // since we don't care about them any more, but groupBy has already eliminated
  // everything but 'word and 'count. Hence, we'll just write them out as 
  // tab-delimited values.
    .write(Tsv(args("output")))
}


/**
 * Run this example, using default arguments if none are specified.
 */
object WordCount {
  val name = "WordCount"
  val message = "Find and count all the words in the KJV."

  def main(args: Array[String]) {
    if (args.length != 0) {
      Run.run(name, message, args)
    } else {
      Run.run(name, message,
        Array("--local", 
          "--input", "data/kjvdat.txt", 
          "--output", "output/kjv-wordcount.txt"))

      Run.printSomeOutput("output/kjv-wordcount.txt")
    }
  }
}
