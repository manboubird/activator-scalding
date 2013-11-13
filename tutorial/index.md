# Activator Scalding Template

## Explore Scalding

This template demonstrates how to build [Scalding](https://github.com/twitter/scalding)-based *Big Data* applications, which can run "locally" on your personal machine or on a [Hadoop](http://hadoop.apache.org) cluster. Actually, these "applications" are more like "scripts".

[Scalding](https://github.com/twitter/scalding) is a Scala API developed at Twitter for distributed data programming that sits on top of the [Cascading](http://www.cascading.org/) Java API, which in turn sits on top of Hadoop's Java API. However, through Cascading, Scalding also offers a *local* mode that makes it easy to run jobs without Hadoop. This greatly simplifies and accelerates learning and testing of applications. It's even "good enough" for small data sets that fit easily on a single machine. 

## Building and Running

Unlike most Activator templates, we compile our scripts, but we don't test the code in the typical way. Instead, we run the scripts with a custom SBT task named `scalding`, as we'll see.

If you want to run any of these scripts in a Hadoop cluster, you'll need to build an all-inclusive "assembly" that contains the contents of most of the dependent jars. That way you have just one jar to deploy to the Hadoop cluster. See the **Running on Hadoop** section below for more details.

### Running Locally with SBT

Start `sbt`. At the prompt, run the task `scalding`. You'll see a multi-line error message like the following that lists the available scripts:

```
[error] Please specify one of the following commands (example arguments shown):
[error]   scalding FilterUniqueCountLimit --input data/kjvdat.txt --output output/kjv
[error]   scalding NGrams --count 20 --ngrams "I love % %" --input data/kjvdat.txt --output output/kjv-ngrams.txt
[error]   scalding TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
[error]   scalding WordCount --input data/kjvdat.txt --output output/kjv-wc.txt
[error] scalding requires arguments.
```

Hence, without providing any arguments, the `scalding` command tells you which scripts are available, what arguments they support with examples that will run with supplied data in the `data` directory. Note that some of the options shown are optional (not indicated); default values will be used. The scripts are listed alphabetically.

Each command should run without error and the output will be written to the file indicated by the `--output` option. You can change the output location to be anything you want.

Let's look at each example. Note that all the scripts are in `src/main/scala/scalding`. Each script discussed next will be in a corresponding `Foo.scala` file and you should look at those files for detailed comments on how they are implemented.

**NOTE:** The first time run any of the following `scalding` tasks, `sbt` will download the dependencies and compile the code. The compilation should go quickly, but the dependencies download can take a while.

#### WordCount

This script implements the well-known *Word Count* algorithm, which is popular as an easy-to-implement, "hello world!" program in Hadoop circles.

In Word Count, a corpus of documents is read, the contents are tokenized into words, and the total count for each word over the entire corpus is computed. The output is sorted by frequency descending.

You invoke the script inside `sbt` like this:

```
scalding WordCount --input data/kjvdat.txt --output output/kjv-wc.txt
```

The `--input` specifies a file containing the King James Version of the Bible. We have included that file; see the [../data/README](../data/README.html) file for more information.

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


#### FilterUniqueCountLimit

This example shows a few techniques:

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

#### NGrams

This (fun) example uses the KJV Bible text to demonstrate how compute *NGrams*, n-word phrases in a corpus. They are commonly used in natural-language processing applications. (see [here](http://en.wikipedia.org/wiki/N-gram) for more details).

A related concept are *Context NGrams*, where phrases containing specific words are desired. 

You invoke the script inside `sbt` like this:

```
scalding NGrams --count 20 --ngrams "I love % %" --input data/kjvdat.txt --output output/kjv-ngrams.txt
```

The `--ngrams` phrase allows optional "context" words, like the "I love" prefix shown here, followed by two words, indicated by the two "%". Hence, you specify the desired `N` implicitly through the number of "%" placeholders and hard-coded words (4-grams, in this example). 

The phrase "% love %" will find all 3-grams with the word "love" in the middle, and so forth. The phrase "% % %" will find all 3-grams, period (i.e., without any "context").

The ngram phrase is translated to a regular expression that also replaces the whitespace with a regular expression for arbitrary whitespace.

**NOTE:** In fact, additional regular expression constructs can be used in this string, e.g., `loves?` will match `love` and `loves`. This can be useful or confusing...

The `--count n` flag means "show the top n most frequent matching ngrams". If not specified, it defaults to 20.

Try different ngram phrases and values of count. Try different data sources.

This example also uses the `debug` pipe to dump output to the console. In this case, you'll see the same output that gets written to the output file, which is the list of the ngrams and their frequencies, sorted by frequency descending.

#### TfIdf

The most complex example implements the *term frequency/inverse document frequecy* algorithm used as part of search indexing, e.g., for the Web. (See [here](http://en.wikipedia.org/wiki/Tf*idf) for more info on this algorithm.)

In a conventional implementation of Tf/Idf, you might load a precomputed document to word matrix: 

```
a[i,j] = freq of the word j in the document with index i 
```

Then, you would compute the Tf/Idf score of each word with respect to each document.

Instead, we'll compute this matrix by first performing a modified *Word Count* on our KJV Bible data, then convert that data to a matrix and proceed from there. The modified *Word Count* will track the source Bible book and `groupBy` the `('book, 'word)` instead of just the `'word`.
 
You invoke the script inside `sbt` like this:

```
scalding TfIdf --n 100 --input data/kjvdat.txt --output output/kjv-tfidf.txt
````

The `--n` argument is optional; it defaults to 100. It specifies how many words to keep for each document. 

### Running on Hadoop

After testing your scripts, you can run them on a Hadoop cluster. You'll first need to build an all-inclusive jar file that contains all the dependencies, including the scala standard library, that aren't already on the cluster.

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

## Next Steps

This template is not a complete Scalding tutorial. To learn more, see the following:

* The Scalding [Wiki](https://github.com/twitter/scalding/wiki). 
* The Scalding [tutorial](https://github.com/twitter/scalding/tree/develop/tutorial) distributed with the [Scalding](https://github.com/twitter/scalding) distribution. 
* Dean Wampler's [Scalding Workshop](https://github.com/deanwampler/scalding-workshop), from which some of this material was adapted.
