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
 * you might load a document to word matrix: 
 *   a[i,j] = freq of the word j in the document i 
 * Then compute the Tf-Idf score of each word w.r.t. to each document.
 * Here, we'll compute this matrix using our KJV Bible data, then convert it a
 * matrix and proceed from there.
 * We'll keep the top N words in each document, See
 * http://en.wikipedia.org/wiki/Tf*idf for more info on this algorithm.
 * 
 * You invoke the script like this:
 *
 *   sbt TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
 *
 * The --n argument is optional; it defaults to 100.
 */
class TfIdf(args : Args) extends Job(args) {
  
  val n = args.getOrElse("nWords", "100").toInt 

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

  // Almost a third of these books are aprocryphal, and hence aren't in the KJV.
  val bookAbbrevsToNames = Map(
    "Act" -> "Acts", 
    "Aes" -> "Additions to Esther", 
    "Amo" -> "Amos", 
    "Aza" -> "Prayer of Azariah", 
    "Bar" -> "Baruch", 
    "Bel" -> "Bel and the Dragon", 
    "Bet" -> "Bel and the Dragon Th", 
    "Ch1" -> "1 Chronicles", 
    "Ch2" -> "2 Chronicles", 
    "Co1" -> "1 Corinthians", 
    "Co2" -> "2 Corinthians", 
    "Col" -> "Colossians", 
    "Dan" -> "Daniel", 
    "Dat" -> "Daniel Th", 
    "Deu" -> "Deuteronomy",
    "Ecc" -> "Ecclesiastes", 
    "Eph" -> "Ephesians", 
    "Epj" -> "Epistle of Jeremiah", 
    "Es1" -> "1 Esdras", 
    "Es2" -> "2 Esdras", 
    "Est" -> "Esther", 
    "Exo" -> "Exodus",
    "Eze" -> "Ezekiel", 
    "Ezr" -> "Ezra", 
    "Gal" -> "Galatians", 
    "Gen" -> "Genesis",
    "Hab" -> "Habakkuk", 
    "Hag" -> "Haggai", 
    "Heb" -> "Hebrews", 
    "Hos" -> "Hosea", 
    "Isa" -> "Isaiah", 
    "Jam" -> "James", 
    "Jda" -> "Judges A", 
    "Jdb" -> "Judges B", 
    "Jde" -> "Jude", 
    "Jdg" -> "Judges",
    "Jdt" -> "Judith", 
    "Jer" -> "Jeremiah", 
    "Jo1" -> "1 John", 
    "Jo2" -> "2 John", 
    "Jo3" -> "3 John", 
    "Job" -> "Job", 
    "Joe" -> "Joel", 
    "Joh" -> "John", 
    "Jon" -> "Jonah", 
    "Jos" -> "Joshua",
    "Jsa" -> "Joshua A", 
    "Jsb" -> "Joshua B", 
    "Kg1" -> "1 Kings", 
    "Kg2" -> "2 Kings", 
    "Lam" -> "Lamentations", 
    "Lao" -> "Laodiceans", 
    "Lev" -> "Leviticus",
    "Luk" -> "Luke", 
    "Ma1" -> "1 Macabees", 
    "Ma2" -> "2 Macabees", 
    "Ma3" -> "3 Macabees", 
    "Ma4" -> "4 Macabees", 
    "Mal" -> "Malachi", 
    "Man" -> "Prayer of Manasseh", 
    "Mar" -> "Mark", 
    "Mat" -> "Matthew", 
    "Mic" -> "Micah", 
    "Nah" -> "Nahum", 
    "Neh" -> "Nehemiah", 
    "Num" -> "Numbers",
    "Oba" -> "Obadiah", 
    "Ode" -> "Odes", 
    "Pe1" -> "1 Peter", 
    "Pe2" -> "2 Peter", 
    "Phi" -> "Philippians", 
    "Plm" -> "Philemon", 
    "Pro" -> "Proverbs", 
    "Psa" -> "Psalms", 
    "Pss" -> "Psalms of Solomon", 
    "Rev" -> "Revelation",
    "Rom" -> "Romans", 
    "Rut" -> "Ruth", 
    "Sa1" -> "1 Samuel", 
    "Sa2" -> "2 Samuel", 
    "Sir" -> "Sirach", 
    "Sol" -> "Song of Solomon", 
    "Sus" -> "Susanna", 
    "Sut" -> "Susanna Th", 
    "Th1" -> "1 Thessalonians", 
    "Th2" -> "2 Thessalonians", 
    "Ti1" -> "1 Timothy", 
    "Ti2" -> "2 Timothy", 
    "Tit" -> "Titus", 
    "Toa" -> "Tobit BA", 
    "Tob" -> "Tobias", 
    "Tos" -> "Tobit S", 
    "Wis" -> "Wisdom", 
    "Zac" -> "Zechariah", 
    "Zep" -> "Zephaniah") 

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
  // Finally, before writing the output, convert the matrix back to a
  // Cascading pipe and replace the bookId with the full name.
  docWordMatrix.hProd(invDocFreqMat).topRowElems(n)
    .pipeAs(('bookId, 'word, 'frequency))
    .mapTo(('bookId, 'word, 'frequency) -> ('book, 'word, 'frequency)){
      tri: (Int,String,Double) => (bookAbbrevsToNames(books(tri._1)), tri._2, tri._3)
    }
    .write(Tsv(args("output")))

  def log2(x : Double) = scala.math.log(x)/scala.math.log(2.0)
}

