import sbt._
import Types._
import Path._
import Keys._

/**
 * Run a Scalding script in local mode.
 * Assumes the scripts are in the src tree and will be compiled by the compile task.
 */
object Scalding {

  val scalding = inputKey[Unit]("Run one of the Scalding scripts.")

  val scaldingTask = scalding := {
    import complete.DefaultParsers._
    val log = streams.value.log
    val args: Vector[String] = spaceDelimited("script>").parsed.toVector
    if (args.size > 0) {
      val mainClass = "com.twitter.scalding.Tool"
      val actualArgs = Vector[String](args.head, "--local") ++ args.tail
      log.info(s"Running scala $mainClass ${actualArgs.mkString(" ")}")
      output(log, 
        (runner in run).value.run(mainClass, Attributed.data((fullClasspath in Runtime).value), 
          actualArgs, streams.value.log))
    } else {
      log.error("Please specify one of the following scripts:")
      val scripts = (sources in Compile).value.map{ file => 
        val s = file.getName
        s.substring(0, s.lastIndexOf("."))
      }
      scripts foreach (s => log.error(s"  $s"))
      log.error("scalding requires arguments.")
    }
  }

  scalding <<= scalding.dependsOn (compile in Compile)

  val scaldingSettings = Seq(scaldingTask)

  private def output(log: Logger, os: Option[String]): Unit = 
    os foreach (s => log.info(s"|  $s"))
}