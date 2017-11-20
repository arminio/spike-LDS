package hmrc.prototypes.trigger

import java.io.File

import awscala.DefaultCredentialsProvider
import com.typesafe.scalalogging.Logger
import lds.from.prototypes.GitHubClient
import lds.from.prototypes.WebHook.PayloadDetails
import org.apache.commons.io.FileUtils
import org.zeroturnaround.zip.ZipUtil

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import scalaj.http._

trait ArtifactManager {
  def getZipAndExplode(githubClient: GitHubClient,
                       payloadDetails: PayloadDetails,
                       branch: String): File
}

object ArtifactManager extends ArtifactManager {
  val logger = Logger(classOf[ArtifactManager])


  val savedZipFilePath = "/tmp/source.zip"


  def getZipAndExplode(githubClient: GitHubClient,
                       payloadDetails: PayloadDetails,
                       branch: String): File = {

    getZip(githubClient, payloadDetails, branch)
    explodeZip()
  }

  def getZip(githubClient: GitHubClient,
             payloadDetails: PayloadDetails, branch: String): Unit = {
    val githubZipUri = getArtifactUrl(payloadDetails, branch)
    logger.info(s"Getting code archive from: $githubZipUri")

    downloadFile(githubClient, githubZipUri, savedZipFilePath, branch)
    logger.info(s"saved archive to: $savedZipFilePath")

  }


  def explodeZip(): File = {
    val explodedZipFile = new File(savedZipFilePath)
    ZipUtil.explode(explodedZipFile)
    logger.info(s"Zip file exploded successfully")
    explodedZipFile
  }


  def downloadFile(githubClient: GitHubClient,
                   url: String,
                   filename: String,
                   branch: String): Unit = {
    val githubPersonalAccessToken = githubClient.selectPersonalApiToken(url)
    retry(5) {
      val resp =
        Http(url)
          .header("Authorization", s"token $githubPersonalAccessToken")
          .option(HttpOptions.followRedirects(true))
          .asBytes
      if (resp.isError) {
        val errorMessage = s"Error downloading the zip file from github:\n${new String(resp.body)}"
        logger.error(errorMessage)
        throw new RuntimeException(errorMessage)
      } else {
        logger.info(s"Response code: ${resp.code}")
        logger.debug(s"Got ${resp.body.size} bytes from $url... saving it to $filename")
        val file = new File(filename)
        FileUtils.deleteQuietly(file)
        FileUtils.writeByteArrayToFile(file, resp.body)
        logger.info(s"Saved file: $filename")
      }
    }
  }

  def retry[T](retryCount: Int)(f: => T): T = {
    Try(f) match {
      case Success(resp) => resp
      case Failure(t) =>
        logger.warn(s"Got error: ${t.getMessage}, retrying $retryCount more times")
        if (retryCount > 0) {
          Thread.sleep(500)
          retry(retryCount - 1)(f)
        } else throw t
    }
  }


  private def getArtifactUrl(payloadDetails:PayloadDetails, branch: String) = {
    payloadDetails.archiveUrl.replace("{archive_format}", "zipball").replace("{/ref}", s"/$branch") //!@ change master to current branch
  }
}
