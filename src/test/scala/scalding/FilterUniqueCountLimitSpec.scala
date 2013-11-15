/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalding

import org.scalatest._
import java.io._
import TestData._

class FilterUniqueCountLimitSpec extends FunSpec {

  val suffixes = Vector("skeptic", "books", "count-star", "limit-N")

  describe("FilterUniqueCountLimit") {
    it("creates empty output files for empty input") {
      val output = "output/fucl-empty"
      val emptyFile = tempFile("fucl")
      com.twitter.scalding.Tool.main(Array(
        "FilterUniqueCountLimit", "--local", "--input", emptyFile.getAbsolutePath(), "--output", output, "--n", "4"))
      for (suffix <- suffixes) 
        assert (io.Source.fromFile(s"$output-$suffix.txt").getLines.size === 0)
    }

    // This is redundantly called four times...
    def run(): String = {
      val outputPrefix = "output/fucl"
      val file = writeTempFile("word-count", religiousText)
      com.twitter.scalding.Tool.main(Array(
        "FilterUniqueCountLimit", "--local", "--input", file.getAbsolutePath(), "--output", outputPrefix, "--n", "4"))
      outputPrefix 
    }

    it("creates a new data file with no references to miracles") {
      val outputPrefix = run()
      val actual = io.Source.fromFile(s"$outputPrefix-skeptic.txt").getLines.toList
      val expected = religiousText.split("\n").toList.filter(s => s.contains("miracle") == false)

      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case (actual, expected) => assert (actual === expected)
      }
    }

    it("creates a new data file with a list of the unique book names, one per line") {
      val outputPrefix = run()
      val actual = io.Source.fromFile(s"$outputPrefix-books.txt").getLines.toList
      val expected = List("Exo", "Gen")

      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case (actual, expected) => assert (actual === expected)
      }
    }

    it("creates a new data file with a count of the number of input lines") {
      val outputPrefix = run()
      val actual = io.Source.fromFile(s"$outputPrefix-count-star.txt").getLines.toList
      val expected = List("6")

      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case (actual, expected) => assert (actual === expected)
      }
    }

    it("creates a new data file with the first N records") {
      val outputPrefix = run()
      val actual = io.Source.fromFile(s"$outputPrefix-limit-N.txt").getLines.toList
      val expected = religiousText.split("\n").toList.take(4)

      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case (actual, expected) => assert (actual === expected)
      }
    }
  }
}
