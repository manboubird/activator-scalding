/*
This script was adapted from "MatrixTutorial6" in the tutorials that come
with the Scalding distribution, which is subject to the Apache License v2.
*/

import com.twitter.scalding._
import com.twitter.scalding.mathematics.Matrix

/*
 * TfIdf
 *
 * In a conventional implementation of term frequency/inverse document frequency,
 * you might load a precomputed document to word matrix: 
 *   a[i,j] = freq of the word j in the document i 
 * Then compute the Tf-Idf score of each word w.r.t. each document.
 * Here, we'll compute this matrix using our KJV Bible data, then convert to a
 * matrix and proceed from there.
 * We'll keep the top N words in each document, See
 * http://en.wikipedia.org/wiki/Tf*idf for more info on this algorithm.
 * 
 * You invoke the script inside sbt like this:
 *
 *   scalding TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
 *
 * The --n argument is optional; it defaults to 100.
 */
class TfIdf(args : Args) extends Job(args) {
  
  val n = args.getOrElse("n", "100").toInt 

  // In the Bible data file, each line has four, "|"-delimited fields:
  // 1. The book of the Bible, e.g., Genesis (abbreviated "Gen").
  // 2. The chapter number.
  // 3. The verse number.
  // 4. The verse itself.
  val kjvSchema = ('book, 'chapter, 'verse, 'text)

  // See WordCount for details on how we tokenize the input docs.
  // In this case, we're going to retain the original Bible book
  // source and group by that. The WordCount example aggregated all
  // counts together.
  val tokenizerRegex = """\W+"""
  
  // We'll need to convert the Bible book names to ids. We could actually compute
  // the unique books and assign each an id (see FilterUniqueCountLimit), but 
  // to simplify things, we'll simply hard-code the abbreviated names used in the 
  // KJV text file.
  val books = Vector(
    "Act", "Amo", "Ch1", "Ch2", "Co1", "Co2", "Col", "Dan", "Deu", 
    "Ecc", "Eph", "Est", "Exo", "Eze", "Ezr", "Gal", "Gen", "Hab", 
    "Hag", "Heb", "Hos", "Isa", "Jam", "Jde", "Jdg", "Jer", "Jo1", 
    "Jo2", "Jo3", "Job", "Joe", "Joh", "Jon", "Jos", "Kg1", "Kg2", 
    "Lam", "Lev", "Luk", "Mal", "Mar", "Mat", "Mic", "Nah", "Neh", 
    "Num", "Oba", "Pe1", "Pe2", "Phi", "Plm", "Pro", "Psa", "Rev", 
    "Rom", "Rut", "Sa1", "Sa2", "Sol", "Th1", "Th2", "Ti1", "Ti2",
    "Tit", "Zac", "Zep")
  
  val booksToIndex = books.zipWithIndex.toMap
  
  val byBookWordCount = Csv(args("input"), separator = "|", fields = kjvSchema)
    .read
    .flatMap('text -> 'word) {
      line : String => line.trim.toLowerCase.split(tokenizerRegex) 
    }
    .project('book, 'word)
    .map('book -> 'bookId)((book: String) => booksToIndex(book))
    .groupBy(('bookId, 'word)){ group => group.size('count) }

  // Now, convert this data to a term frequency matrix, using Scalding Matrix API.
  //   a[i,j] = freq of the word j in the document i 

  import Matrix._

  val docSchema = ('bookId, 'word, 'count)

  val docWordMatrix = byBookWordCount
    .toMatrix[Long,String,Double](docSchema)

  // Compute the overall document frequency of each word.
  // docFreq(i) will be the total count for word i over all docs.
  val docFreq = docWordMatrix.sumRowVectors

  // Compute the inverse document frequency vector.
  // L1 normalize the docFreq: 1/(|a| + |b| + ...)
  // Use 1/log(x), rather than 1/x, for better numerical stability. 
  val invDocFreqVct = 
    docFreq.toMatrix(1).rowL1Normalize.mapValues( x => log2(1/x) )

  // Zip the row vector along the entire document - word matrix.
  val invDocFreqMat = 
    docWordMatrix.zip(invDocFreqVct.getRow(1)).mapValues(_._2)

  // Multiply the term frequency with the inverse document frequency
  // and keep the top N words. "hProd" is the Hadamard product, i.e.,
  // multiplying elementwise, rather than row vector times column vector.
  // Next, before writing the output, convert the matrix back to a
  // Cascading pipe and replace the bookId with the abbreviated name.
  // Note the "mapTo"; it's like map, but the later keeps all the original
  // input fields and adds the new one(s) output from map. MapTo tosses all
  // the fields not passed in. It's more efficient than map(...).project(...),
  // which would be functionally equivalent.
  val out1 = docWordMatrix.hProd(invDocFreqMat).topRowElems(n)
    .pipeAs(('bookId, 'word, 'frequency))
    .mapTo(('bookId, 'word, 'frequency) -> ('book, 'word, 'frequency)){
      tri: (Int,String,Double) => (books(tri._1), tri._2, tri._3)
    }

  // Finally, before writing the output, let's see how joins work, which 
  // we'll use to bring in a table of the book abbreviations and the full names.
  // Note that almost a third of these books in that data set are aprocryphal 
  // and hence aren't in the KJV.
  // This data is tab-delimited and we'll just hard code a default path to it
  // unless you use the option we didn't document above!
  val abbrevToNameFile = args.getOrElse("abbrevs-to-names", "data/abbrevs-to-names.tsv")
  val abbrevToName = Tsv(abbrevToNameFile, fields = ('abbrev, 'name)).read

  // The "tiny" file is the abbreviations. Knowing which one is smallest is 
  // important, because Cascading can use a optimization known as a map-side 
  // join. In short, if the small data set can fit in memory, that data can
  // be cached in the "map" process memory and the join can be performed as
  // the larger data set streams through. See this description for Hive, 
  // which implements the optimization, too: 
  // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+JoinOptimization#LanguageManualJoinOptimization-PriorSupportforMAPJOIN)
  // The pair tuple passed to the inner join method "joinWithTiny" lists one 
  // or more fields from the left-hand pipe and a corresponding number of 
  // fields from the right-hand pipe to join on. Here, we just join on a
  // single field from each pipe. If it were two, we would pass an argument 
  // like (('l1, l2') -> ('r1, 'r2)). Note the nested tuples within the outer
  // pair tuple.
  // After joining, we do a final projection and then write the output.
  out1.joinWithTiny('book -> 'abbrev, abbrevToName)
    .project('name, 'word, 'frequency)
    .write(Tsv(args("output")))

  def log2(x : Double) = scala.math.log(x)/scala.math.log(2.0)
}


/**
 * Run this example, using default arguments if none are specified.
 */
object TfIdf {
  val name = "TfIdf"
  val message = 
    "Compute term frequency-inverse document frequency on the KJV."

  def main(args: Array[String]) {
    if (args.length != 0) {
      Run.run(name, message, args)
    } else {
      Run.run(name, message,
        Array("--local", "--n", "100",
          "--input", "data/kjvdat.txt", 
          "--output", "output/kjv-tfidf.txt"))

      Run.printSomeOutput("output/kjv-tfidf.txt")
    }
  }
}
