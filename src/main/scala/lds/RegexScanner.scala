package lds

class RegexScanner(regexString: String) {

  val compiledRegex = s"""$regexString""".r

  def scan(text: String): Seq[ScanResults] = {

    val matchedResults = text
      .lines.toSeq.par
      .zipWithIndex
      .filter { case (lineText, lineNumber) =>
        lineText match {
          case compiledRegex(_*) => true
          case _ => false
        }
      }.map { case (lineText, lineNumber) =>
      MatchedResult(lineText, lineNumber)
    }

    matchedResults match {
      case results if results.nonEmpty => results
      case _ => Seq(MatchNotFound)
    }

  }.seq


}
