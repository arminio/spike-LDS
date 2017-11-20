package lds

sealed trait ScanResults

case object MatchNotFound extends ScanResults
case class MatchedResult(lineText: String, lineNumber: Int) extends ScanResults
