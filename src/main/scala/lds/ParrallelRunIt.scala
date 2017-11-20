package lds

import java.io.File
import java.nio.charset.StandardCharsets
import java.util

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{FileFileFilter, TrueFileFilter}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object ParrallelRunIt {

  val regexList: Seq[String] = Seq(
//    ".*Gesource.*",
//    ".*Splunk.*",
//    ".*Aesource.*",
//    ".*Besource.*",
//    ".*Cesource.*",
//    ".*Desource.*",
//    ".*Eesource.*",
//    ".*Fesource.*",
//    ".*hesource.*",
    ".*Resource.*"
  )
  val scanners = regexList.map(new RegexScanner(_))
  var counter = 0l
  var summary: Seq[ScanResults] = Nil

  val path = "/Users/armin/temp/unziped/puppet-master"


  def main(args: Array[String]): Unit = {
    val s = System.currentTimeMillis()
    val filesAndDirs: util.Collection[File] =
      FileUtils.listFilesAndDirs(
        new File(path),
        FileFileFilter.FILE,
        TrueFileFilter.INSTANCE
      )

    val results = filesAndDirs.asScala.filterNot(_.isDirectory).par.flatMap { file =>
      counter = counter + 1

      val fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
      scanners.map { scanner =>
        val scanResults: Seq[(File, ScanResults)] = scanner.scan(fileContent).map(sr => (file, sr))
        if (scanResults.exists(_._2 != MatchNotFound)) {
          scanResults
        } else Nil
      }


    }.flatten.seq

    val e = System.currentTimeMillis()
    println(results.mkString(",\n"))
    println(s"${(e - s) / 1000.0}s for $counter files")
  }
}
