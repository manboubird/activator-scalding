/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalding

import org.scalatest._
import java.io._
import TestData._

class TfIdfSpec extends FunSpec {

  describe("TfIdf") {
    it("creates empty output for empty input") {
      val output = "output/tfidf-empty.txt"
      val emptyFile = tempFile("tfidf")
      com.twitter.scalding.Tool.main(Array(
        "TfIdf", "--local", "--n", "10", "--input", emptyFile.getAbsolutePath(), "--output", output))
      assert (io.Source.fromFile(output).getLines.size === 0)
    }

    it("creates TfIdf sorted descending by count for non-empty input") {
      val output = "output/TfIdf-activator.txt"
      val file = writeTempFile("TfIdf", religiousText)
      com.twitter.scalding.Tool.main(Array(
        "TfIdf", "--local", "--n", "10", "--input", file.getAbsolutePath(), "--output", output))

      val actual = io.Source.fromFile(output).getLines.toList.map{ s => 
        val words = s.split("\t")
        (words(0), words(1), words(2).toDouble)
      }
      val expected = religiousTfIdfExpected
      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
        case ((actualBook, actualWord, actualFreq), (expectedBook, expectedWord, expectedFreq)) => 
          assert (actualBook === expectedBook)
          assert (actualWord === expectedWord)
          assert (math.abs(actualFreq - expectedFreq) < 0.0001)
      }
    }
  }
}
