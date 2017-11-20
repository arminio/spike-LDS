package lds.config

import java.nio.charset.StandardCharsets

import awscala.s3.{Bucket, S3Client}
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

sealed trait ConfigReader {
  def readConfig(configName : String): Seq[String]
}

class StaticConfigReader() extends ConfigReader {
  override def readConfig(configName: String) =
    StaticConfigurations.get(configName).getOrElse(throw new Exception(s"No config with name $configName"))
}

class S3ConfigReader() extends ConfigReader {

  lazy val s3Client = new S3Client(AwsCredentials.credentialsProvider)

  val codeInspectionConfigBucket = "mdtp-code-inspection"

  def readConfig(configName : String): Seq[String] = {
    s3Client.get(Bucket(codeInspectionConfigBucket), configName)
      .map(s3Object => IOUtils.readLines(s3Object.content, StandardCharsets.UTF_8).asScala)
      .getOrElse(throw new RuntimeException(s"no configuration found (Bucket: $codeInspectionConfigBucket, File: $configName)"))
      .toSeq
  }

}
