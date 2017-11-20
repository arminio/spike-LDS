package lds

import org.scalatest.{FreeSpec, Matchers}

class RegexScannerSpec extends FreeSpec with Matchers {

  "scan" - {
    "should look for a regex in a given text" - {
      "and find return the line number matching the regex" in {

        val text =
          """nothing matching here
            |this matches the regex
            |this matches the regex too
            |nothing matching here
            |""".stripMargin
        val regex = "^.*(matches).*"

        new RegexScanner(regex).scan(text) should
          contain theSameElementsAs Seq(
          MatchedResult("this matches the regex", lineNumber = 1),
          MatchedResult("this matches the regex too", lineNumber = 2)
        )
      }

      "and report MatchNotFound if text doesn't have matching lines for the given regex" in {

        val text = "this is a test"
        val regex = "^.*(was).*"

        new RegexScanner(regex).scan(text) should contain theSameElementsAs Seq(MatchNotFound)
      }
    }
  }
}
