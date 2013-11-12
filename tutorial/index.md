# Activator Scalding Template

## Explore Scalding

This template demonstrates how to build [Scalding](https://github.com/twitter/scalding)-based *Big Data* applications, which can run "locally" on your personal machine or on a [Hadoop](http://hadoop.apache.org) cluster. Actually, these "applications" are more like "scripts".

[Scalding](https://github.com/twitter/scalding) is a Scala API developed at Twitter for distributed data programming that sits on top of the [Cascading](http://www.cascading.org/) Java API, which in turn sits on top of Hadoop's Java API. However, through Cascading, Scalding also offers a *local* mode that makes it easy to run jobs without Hadoop. This greatly simplifies and accelerates learning and testing of applications. It's even "good enough" for small data sets that fit easily on a single machine. 

## Building and Running

Unlike most Activator templates, we compile our scripts, but we don't test the code in the typical way. Instead, we run the scripts with a custom SBT task named `scalding`, as we'll see.

If you want to run any of these scripts in a Hadoop cluster, you'll need to build an all-inclusive "assembly" that contains the contents of most of the dependent jars. That way you have just one jar to deploy to the Hadoop cluster. See the **Running on Hadoop** section below for more details.

### Running Locally with SBT

Start `sbt`. At the prompt, run the task `scalding`. You'll see a multi-line error message like the following that lists the available scripts:


The commands should run without error. The `sbt assembly` command first runs an `update` task, which downloads all the dependencies, using the specification in `project/Build.scala`. You'll see lots of messages as it tries different repositories. Note that these dependencies will be downloaded to your `$HOME/.ivy2` directory (on *nix systems). **This may take a while to run!!**

Next, the `assembly` task builds an all-inclusive jar file that includes all the dependencies, including Scalding and Hadoop. This jar file makes it easier to run Scalding scripts on Hadoop, because it simplifies working with dependency jars and the `CLASSPATH`. The output of `assembly` is `target/ScaldingWorkshop-X.Y.Z.jar`, where `X.Y.Z` will be the current version number for the workshop.

For completeness, note also that the version of sbt itself is specified in `project/build.properties`. There is also a `project/plugins.sbt` file that specifies some sbt plugins we use. 

Finally, the `run` Scala script takes a moment to compile the Scalding script and then run it. The output is written to `output/SanityCheck0.txt`. (What's in that file?)

If you have Ruby installed on your system, there is a port of `run` in Ruby called `run.rb`. To use it, just replace the `run` command above in the bash case or for windows, use `ruby run.rb` instead of `scala run`.
 
See the Appendix below for "optional installs", if you decide to use Scalding after the tutorial.

## Next Steps

You can now start with the workshop itself. Go to the companion [Workshop page](https://github.com/thinkbiganalytics/scalding-workshop/blob/master/Workshop.html).

## Notes on Releases

### V0.3.0 

Moved to Scala v2.10.2 and Scalding v0.8.6. Completely reworked the build process and the script running process. Refined many of the exercises.

### V0.2.1 

Added a file missing from distribution. Refined the run scripts to work better with different Java versions.

### V0.2 

Refined several exercises and fixed bugs. Added `Makefile` for building releases. (Since removed...)

### V0.1 

First release for the StrangeLoop 2012 workshop.


## For Further Information

See the [Scalding GitHub page](https://github.com/twitter/scalding) for more information about Scalding. The [wiki](https://github.com/twitter/scalding/wiki) is indispensable.

I'm [Dean Wampler](mailto:dean@deanwampler.com) from [Concurrent Thought](http://concurrentthought.com). I prepared this workshop. Send me email with [questions about the workshop](mailto:dean@deanwampler.com?subject=Question%20about%20your%20Scalding%20Workshop) or for [information about consulting and training](mailto:dean@deanwampler.com?subject=Hiring%20Dean%20Wampler) on Scala, Scalding, or other Hadoop and *Big Data* topics.

Some of the data used in these exercises was obtained from [InfoChimps](http://infochimps.com).

**NOTE:** The first version of this workshop was written while I worked at Think Big Analytics. The original and now obsolete fork of the workshop is [here](https://github.com/ThinkBigAnalytics/scalding-workshop).

**Dean Wampler**<br/>
[dean@concurrentthought.com](mailto:dean@concurrentthought.com?subject=Question%20about%20your%20Scalding%20Workshop)<br/>
[@deanwampler](https://twitter.com/deanwampler)<br/>

## Appendix - Optional Installs

If you're serious about using Scalding, you should clone and build the Scalding repo itself. We'll talk briefly about it in the workshop, but it isn't required.

### Scalding from GitHub

Clone [Scalding from GitHub](https://github.com/twitter/scalding). Using `bash` and assuming you'll clone it into `$HOME/fun`:

    cd $HOME/fun
    git clone https://github.com/twitter/scalding.git

Windows is similar.

### Ruby v1.8.7 or v1.9.X

Ruby is used as a platform-independent language for driver scripts by Scalding (e.g., their `scripts/scald.rb`). See [ruby-lang.org](http://ruby-lang.org) for details on installing Ruby. Either version 1.8.7 or 1.9.X will work.

### Build Scalding

Build Scalding according to its [Getting Started](https://github.com/twitter/scalding/wiki/Getting-Started) page. By default, Twitter builds with Scala v2.9.3, but Scalding builds with 2.10.2 and the `project/Build.scala` file can be edited for this version. 

Edit `project/Build.scala`. Near the top, you'll see a line `scalaVersion := 2.9.2` and next to it, a commented line for version 2.10.0. Comment out the line with 2.9.2 and uncomment the 2.10.0 line, then change the last zero to "2". Save your changes.

Now, here is a synopsis of the build steps. Using `bash`: 

    cd $HOME/fun/scalding
    sbt update
    sbt assembly

On Windows:

    cd C:\fun\scalding
    sbt update
    sbt assembly

(The Getting Started page says to build the `test` target between `update` and `assembly`, but the later builds `test` itself.)

### Sanity Check

Once you've built Scalding, run the following command as a sanity check to ensure everything is setup properly. Using `bash`: 

    cd $HOME/fun/scalding
    scripts/scald.rb --local tutorial/Tutorial0.scala

On Windows:

    cd C:\fun\scalding
    ruby scripts\scald.rb --local tutorial/Tutorial0.scala

