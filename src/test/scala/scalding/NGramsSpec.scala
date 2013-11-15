/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalding

import org.scalatest._
import java.io._
import TestData._

class NGramsSpec extends FunSpec {

  describe("NGrams") {
    it("creates empty output for empty input") {
      val output = "output/ngrams-empty.txt"
      val emptyFile = tempFile("ngrams")
      com.twitter.scalding.Tool.main(Array(
        "NGrams", "--local", "--count", "5", "--ngrams", "activator % % %", "--input", emptyFile.getAbsolutePath(), "--output", output))
      assert (io.Source.fromFile(output).getLines.size === 0)
    }

    it("creates ngrams sorted descending by count for non-empty input") {
      val output = "output/ngrams-activator.txt"
      val file = writeTempFile("ngrams", activatorText)
      com.twitter.scalding.Tool.main(Array(
        "NGrams", "--local", "--count", "5", "--ngrams", "activator % % %", "--input", file.getAbsolutePath(), "--output", output))

      val actual = io.Source.fromFile(output).getLines.toList
      val expected = List(activatorNGramExpected)
      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case (actual, expected) => assert (actual === expected)
      }
    }
  }
}
