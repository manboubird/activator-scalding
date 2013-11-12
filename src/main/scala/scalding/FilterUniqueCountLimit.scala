import com.twitter.scalding._

/**
 * FilterUniqueCountLimit
 *
 * This example shows a few techniques:
 * 1. How to split a data stream into several flows, each for a specific calculation.
 * 2. How to filter records (like SQL's "WHERE" clause).
 * 3. How to find unique values (like SQL's "DISTINCT").
 * 4. How to count all records (like SQL's "COUNT(*)").
 * 5. How to limit output (like SQL's "LIMIT n" clause).
 * You invoke the script like this:
 *
 *   sbt FilterUniqueCountLimit --input data/kjvdat.txt --output output/kjv
 *
 * In this case, the output is used as a prefix for 3 output files.
 * (Use any output value you want.)
 */

class FilterUniqueCountLimit(args : Args) extends Job(args) {

  // In the Bible data file, each line has four, "|"-delimited fields:
  // 1. The book of the Bible, e.g., Genesis (abbreviated "Gen").
  // 2. The chapter number.
  // 3. The verse number.
  // 4. The verse itself.
  val kjvSchema = ('book, 'chapter, 'verse, 'text)

  val outputPrefix = args("output")

  // The initial stream:.
  // Because it's "pipe" delimited (i.e., "|"), use the Csv input class
  // (normally for comma-separated values), which lets you override the separator.
  // Curiously, the similar Tsv class doesn't support this override.
  val bible = Csv(args("input"), separator = "|", fields = kjvSchema)
      .read

  // Split the pipe into a first stream for skeptics who don't believe the
  // bible is divinely inspired; it removes all verses that include the word 
  // "miracle".
  new RichPipe(bible)
      .filter('text) { t:String => t.contains("miracle") == false }
      .write(Csv(s"$outputPrefix-skeptic.txt", separator = "|"))

  // Split the pipe into a stream to find the unique names of the books 
  // of the Bible. Note that we project just the 'book field first, then
  // compute the unique values. (When it's easier, you can also discard(...)
  // fields you don't want.)
  // (Only one field is written, so the separator doesn't matter.)
  new RichPipe(bible)
      .project('book)
      .unique('book)
      .write(Tsv(s"$outputPrefix-books.txt"))  

  // Another split used to implement "COUNT(*)".
  // The reducers(2) two argument shows how you can tell Cascading how many reducers
  // to use, when running this script in a Hadoop cluster. In this example, lots of 
  // reducers is useful to parallelize this counting, if the data set were huge.
  new RichPipe(bible)
      .groupAll { _.size('countstar).reducers(2) }
      .write(Tsv(s"$outputPrefix-count-star.txt"))  

  // Yet another split used to implement "LIMIT N".
  new RichPipe(bible)
      .limit(1000)
      .write(Tsv(s"$outputPrefix-limit-N.txt"))  
}
