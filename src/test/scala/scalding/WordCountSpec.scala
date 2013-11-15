/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalding

import org.scalatest._
import java.io._
import TestData._

class WordCountSpec extends FunSpec {

  describe("WordCount") {
    it("creates empty output for empty input") {
      val output = "output/word-count-empty.txt"
      val emptyFile = tempFile()
      com.twitter.scalding.Tool.main(Array(
        "WordCount", "--local", "--input", emptyFile.getAbsolutePath(), "--output", output))
      assert (io.Source.fromFile(output).getLines.size === 0)
    }

    it("creates tab-delimited word/count pairs, one per line for non-empty input") {
      val output = "output/word-count-activator.txt"
      val file = tempFile()
      val writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
      writer.write(activatorText)
      writer.close()
      com.twitter.scalding.Tool.main(Array(
        "WordCount", "--local", "--input", file.getAbsolutePath(), "--output", output))

      val actual = io.Source.fromFile(output).getLines.toList
      val expected = io.Source.fromFile("src/test/resources/word-count-activator-expected.txt").getLines.toList
      assert (actual.size === expected.size)
      actual.zip(expected).foreach {
          case (actual, expected) => assert (actual === expected)
        }
    }
  }
}
