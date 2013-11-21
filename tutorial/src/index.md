# Activator Scalding Template

## Explore Scalding

This template demonstrates how to build and run [Scalding](https://github.com/twitter/scalding)-based *Big Data* applications for [Hadoop](http://hadoop.apache.org). You can also run them "locally" on your personal machine, which we will do here, for convenient development and testing. Actually, these "applications" are more like "scripts" that traditional, multi-file applications.

[Scalding](https://github.com/twitter/scalding) is a Scala API developed at Twitter for distributed data programming that sits on top of the [Cascading](http://www.cascading.org/) Java API, which in turn sits on top of Hadoop's Java API. However, through Cascading, Scalding also offers a *local* mode that makes it easy to run jobs without Hadoop. This greatly simplifies and accelerates learning and testing of applications. It's even "good enough" for small data sets that fit easily on a single machine. 

## Building and Running

Invoke the Activator *compile* and *test* commands to build the code and run the unit tests. Compiling will take a few minutes as the dependent libraries are downloaded. The tests should pass without error.

Now you can invoke *run* to try it out. An *NGrams* algorithm is used to find all the 4-word ("4-gram") phrases in the King James Version of the Bible of the form "% love % %", where the "%" are wild cards. In other words, all 4-grams are found with "love" as the second word.

## The NGrams Script

Let's see how the *NGrams* Script works. Open <a href="#code/src/main/scala/scalding/NGrams.scala">NGrams.scala</a>. Here is the entire script, with the comments removed:

```
import com.twitter.scalding._

class NGrams(args : Args) extends Job(args) {
  
  val ngramsArg = args.list("ngrams").mkString(" ").toLowerCase
  val ngramsRE = ngramsArg.trim
    .replaceAll("%", """ (\\p{Alnum}+) """)
    .replaceAll("""\s+""", """\\p{Space}+""").r
  val numberOfNGrams = args.getOrElse("count", "20").toInt

  val countReverseComparator = 
    (tuple1:(String,Int), tuple2:(String,Int)) => tuple1._2 > tuple2._2
      
  val lines = TextLine(args("input"))
    .read
    .flatMap('line -> 'ngram) { 
      text: String => ngramsRE.findAllIn(text.trim.toLowerCase).toIterable 
    }
    .discard('offset, 'line)
    .groupBy('ngram) { _.size('count) }
    .groupAll { 
      _.sortWithTake[(String,Int)](
        ('ngram,'count) -> 'sorted_ngrams, numberOfNGrams)(countReverseComparator)
    }
    .debug
    .write(Tsv(args("output")))
}
```

Let's walk through this code. 

```
import com.twitter.scalding._

class NGrams(args : Args) extends Job(args) {
  ...
```

We start with the Scalding imports we need, then declare a class `NGrams` that subclasses a `Job` class, which provides a `main` routine and other runtime context support (such as Hadoop integration). Our class must take a list of command-line arguments, which are processed for us by Scalding's `Args` class. We'll use these to specify where to find input, where to write output, and handle other configuration options.

```
  ...
  val ngramsArg = args.list("ngrams").mkString(" ").toLowerCase
  val ngramsRE = ngramsArg.trim
    .replaceAll("%", """ (\\p{Alnum}+) """)
    .replaceAll("""\s+""", """\\p{Space}+""").r
  val numberOfNGrams = args.getOrElse("count", "20").toInt
  ...
```

Before we create our *dataflow*, a series of *pipes* that provide data processing, we define a values that we'll need. The user specifies the NGram pattern they want, such as the "% love % %" used in our *run* example. The `ngramsRE` takes that NGram specification and turns it into a regular expression that we need. The "%" are converted into patterns to find any word and any runs of whitespace are generalized for all whitespace. Finally, we get the command line argument for the number of most frequently occurring NGrams to find, which defaults to 20 if not specified.

```
  ...
  val countReverseComparator = 
    (tuple1:(String,Int), tuple2:(String,Int)) => tuple1._2 > tuple2._2
  ...
```

The `countReverseComparator` function will be used to rank our found NGrams by frequency of occurrence, descending. The count of occurrences will be the second field in each tuple.


```
  ...
  val lines = TextLine(args("input"))
    .read
    .flatMap('line -> 'ngram) { 
      text: String => ngramsRE.findAllIn(text.trim.toLowerCase).toIterable 
    }
    .discard('offset, 'line)
    ...
```

Now our dataflow is created. A `TextLine` object is used to read each "record", a line of text as a single "field". Hence, the records are newline (`\n`) separated. It reads the file specified by the `--input` argument (processed by the `args` object). 

Note that a flaw with our implementation is that NGrams across line boundaries won't be found, because we process each line separately. However, the text for the King James Version of Bible that we are using has each verse on its own line. It wouldn't make much sense to compute NGrams across verses, so this limitation is not an issue for this particular data set.

Next, we call `flatMap` on each line record, converting it to zero or more output records, one per NGram found. Of course, some lines won't have a matching NGram. We use our regular expression to tokenize each line, and also trim leading and trailing whitespace and convert to lower case. 

A scalding API convention is to use the first argument list to a function to specify the field names to input to the function and name the new fields output. In this case, we input just the line field, named `'line` (a Scala *symbol*) and name each found NGram `'ngram`. Note who these field names are specified using a tuple.

Finally in this section, we discard the fields we no longer need. Operations like `flatMap` and `map` append the new fields to the existing fields. We no longer need the `'line` and `TextLine` also added a line number field to the input, named `'offset`. 

```
    ...
    .groupBy('ngram) { _.size('count) }
    .groupAll { 
      _.sortWithTake[(String,Int)](
        ('ngram,'count) -> 'sorted_ngrams, numberOfNGrams)(countReverseComparator)
    }
    ...
}

```

If we want to rank the found NGrams by their frequencies, we need to get all occurrences of a given NGram together. Hence, we use a `groupBy` operation to group over the `'ngram` fields. To sort and output the tope `numberOfNGrams`, we group *all* together, then use a special Scalding function that combines sorting with "taking", i.e., just keeping the top N values after sorting.


```
    ...
    .debug
    .write(Tsv(args("output")))
}
```

The `debug` function dumps the current stream of data to the console, which is useful for debugging. Don't do this for massive data sets!!

Finally, we write the results as tab-separated values to the location specified by the `--output` command-line argument.

To recap, look again at the whole listing above. It's not very big! For what it does and compared to typical code bases you might work with, this is incredibly concise and powerful code.

There are additional capabilities built into this template. If you know how to use [sbt](http://www.scala-sbt.org/), the standard Scala build tool, proceed to the next sections of this tutorial for more information.


## Running Locally with SBT

Start `sbt`. At the prompt, invoked the `run` task, which is the same task that the Activator *run* command invoked. It calculates all the 4-grams (4-word phrases) in the King James Version of the Bible of the form "% love % %", where the "%" are wild cards. In other words, all 4-grams are found with "love" as the second word.

There are three additional Scalding examples in this template. To run all the examples, with the ability to vary the input arguments, use the `scalding` task included with this project. 

At the `sbt` prompt, type `scalding`. You'll see the following:

```
> scalding
[error] Please specify one of the following commands (example arguments shown):
[error]   scalding FilterUniqueCountLimit --input data/kjvdat.txt --output output/kjv
[error]   scalding NGrams --count 20 --ngrams "I love % %" --input data/kjvdat.txt --output output/kjv-ngrams.txt
[error]   scalding TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
[error]   scalding WordCount --input data/kjvdat.txt --output output/kjv-wc.txt
[error] scalding requires arguments.
```

Hence, without providing any arguments, the `scalding` command tells you which scripts are available and the arguments they support with examples that will run with supplied data in the `data` directory. Note that some of the options shown are optional (not indicated); default values will be used. The scripts are listed alphabetically and they include the `NGrams` script that the `run` task invokes.

Each command should run without error and the output will be written to the file indicated by the `--output` option. You can change the output location to be anything you want.

Let's look at each example. Note that all the scripts are in `src/main/scala/scalding`. Each script discussed next will be in a corresponding `<script-name>.scala` file and you should look at those files for detailed comments on how they are implemented.

**NOTE:** The first time run any of the following `scalding` tasks, `sbt` will download the dependencies and compile the code. The compilation should go quickly, but the dependencies download can take a while.

## WordCount

Open Open <a href="#code/src/main/scala/scalding/WordCount.scala">WordCount.scala</a>, which implements the well-known *Word Count* algorithm, which is popular as an easy-to-implement, "hello world!" program in Hadoop circles.

In Word Count, a corpus of documents is read, the contents are tokenized into words, and the total count for each word over the entire corpus is computed. The output is sorted by frequency descending.

You invoke the script inside `sbt` like this:

```
scalding WordCount --input data/kjvdat.txt --output output/kjv-wc.txt
```

The `--input` specifies a file containing the King James Version of the Bible. We have included that file; see the `data/README` file for more information.

Each line actually has the "schema"

```
Abbreviated name of the book of the Bible (e.g., Gen) | chapter | verse | text
```

For example,

```
Gen|1|1| In the beginning God created the heaven and the earth.~
```

We just treat the whole line as text. A nice exercise is to *project* out just the `text` field. See the other scripts for examples of how to do this.

The `--output` argument specifies where the results are written. You just see a few log messages written to the `sbt` console. You can use any path you want for this output.


## FilterUniqueCountLimit

Open <a href="#code/src/main/scala/scalding/FilterUniqueCountLimit.scala">FilterUniqueCountLimit.scala</a>, which shows a few useful techniques:

1. How to split a data stream into several flows, each for a specific calculation.
2. How to filter records (like SQL's "WHERE" clause).
3. How to find unique values (like SQL's "DISTINCT").
4. How to count all records (like SQL's "COUNT(*)").
5. How to limit output (like SQL's "LIMIT n" clause).

You invoke the script inside `sbt` like this:

```
scalding FilterUniqueCountLimit --input data/kjvdat.txt --output output/kjv
```

In this case, the `--output` is actually used as a prefix for 4 output files, the results of numbers 2-5 above.

## NGrams

We discussed <a href="#code/src/main/scala/scalding/NGrams.scala">NGrams.scala</a> before. It uses the KJV Bible text to demonstrate how to compute *NGrams*, n-word phrases in a corpus. They are commonly used in natural-language processing applications. (see [here](http://en.wikipedia.org/wiki/N-gram) for more details).

A related concept are *Context NGrams*, where phrases containing specific words are desired. 

You invoke the script inside `sbt` like this:

```
scalding NGrams --count 20 --ngrams "I love % %" --input data/kjvdat.txt --output output/kjv-ngrams.txt
```

The `--ngrams` phrase allows optional "context" words, like the "I love" prefix shown here, followed by two words, indicated by the two "%". Hence, you specify the desired `N` implicitly through the number of "%" placeholders and hard-coded words (4-grams, in this example). 

The phrase "% love %" will find all 3-grams with the word "love" in the middle, and so forth. The phrase "% % %" will find all 3-grams, period (i.e., without any "context").

The NGram phrase is translated to a regular expression that also replaces the whitespace with a regular expression for arbitrary whitespace.

**NOTE:** In fact, additional regular expression constructs can be used in this string, e.g., `loves?` will match `love` and `loves`. This can be useful or confusing...

The `--count n` flag means "show the top n most frequent matching NGrams". If not specified, it defaults to 20.

Try different NGram phrases and values of count. Try different data sources.

This example also uses the `debug` pipe to dump output to the console. In this case, you'll see the same output that gets written to the output file, which is the list of the NGrams and their frequencies, sorted by frequency descending.

## TfIdf

Open <a href="#code/src/main/scala/scalding/TfIdf.scala">TfIdf.scala</a>, our most complex example script. It implements the *term frequency/inverse document frequency* algorithm used as part of  the indexing process for document or Internet search engines. (See [here](http://en.wikipedia.org/wiki/Tf*idf) for more information on this algorithm.)

In a conventional implementation of Tf/Idf, you might load a precomputed document to word matrix: 

```
a[i,j] = frequency of the word j in the document with index i 
```

Then, you would compute the Tf/Idf score of each word with respect to each document.

Instead, we'll compute this matrix by first performing a modified *Word Count* on our KJV Bible data, then convert that data to a matrix and proceed from there. The modified *Word Count* will track the source Bible book and `groupBy` the `('book, 'word)` instead of just the `'word`.
 
You invoke the script inside `sbt` like this:

```
scalding TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
````

The `--n` argument is optional; it defaults to 100. It specifies how many words to keep for each document. 

## Running on Hadoop

After testing your scripts, you can run them on a Hadoop cluster. You'll first need to build an all-inclusive jar file that contains all the dependencies, including the Scala standard library, that aren't already on the cluster.

The `sbt assembly` command first runs an `update` task, if missing dependencies need to be downloaded. Then the task builds the all-inclusive jar file, which is written to `target/scala-2.10/activator-scalding-X.Y.Z.jar`, where `X.Y.Z` will be the current version number for this project.

One the jar is built and assuming you have the `hadoop` command installed on your system (or the server to which you copy the jar file...), the following command syntax will run one of the scripts

```
hadoop jar target/scala-2.10/activator-scalding-X.Y.Z.jar SCRIPT_NAME \ 
  [--hdfs | --local ] [--host JOBTRACKER_HOST] \ 
  --input INPUT_PATH --output OUTPUT_PATH \ 
  [other-args] 
```

Here is an example for `NGrams`, using HDFS, not the local file system, and assuming the JobTracker host is determined from the local configuration files, so we don't have to specify it:

```
hadoop jar target/scala-2.10/activator-scalding-X.Y.Z.jar NGrams \ 
  --hdfs  --input /data/docs --output output/wordcount \ 
  --count 100 --ngrams "% loves? %"
```

Note that when using HDFS, Hadoop treats all paths as *directories*. So, all the files in an `--input` directory will be read. In `--local` mode, the paths are interpreted as *files*.

An alternative to running the `hadoop` command directly is to use the `scald.rb` script that comes with Scalding distributions. See the [Scalding](https://github.com/twitter/scalding) website for more information.

## Going Forward from Here

This template is not a complete Scalding tutorial. To learn more, see the following:

* The Scalding [Wiki](https://github.com/twitter/scalding/wiki). 
* The Scalding [tutorial](https://github.com/twitter/scalding/tree/develop/tutorial) distributed with the [Scalding](https://github.com/twitter/scalding) distribution. 
* Dean Wampler's [Scalding Workshop](https://github.com/deanwampler/scalding-workshop), from which some of this material was adapted.
* See [Typesafe](http://typesafe.com) for more information about our products and services. 
* See [Typesafe Activator](http://typesafe.com/activator) to find other Activator templates.

