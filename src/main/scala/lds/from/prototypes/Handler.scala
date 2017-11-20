package lds.from.prototypes

import java.io.File
import java.util

import awscala.DefaultCredentialsProvider
import awscala.s3.S3Client
import com.amazonaws.services.lambda.runtime.Context
import com.typesafe.scalalogging.Logger
import hmrc.prototypes.trigger.ArtifactManager
import lds.WebhookSecretChecker
import lds.config.{AwsCredentials, ConfigReader, StaticConfigReader}
import lds.from.prototypes.MiscHelper.runAndRespond
import org.apache.commons.lang.builder.ToStringBuilder

import scala.beans.BeanProperty
import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, mapAsScalaMapConverter}
import scala.util.{Failure, Success, Try}

class Handler {


  lazy val webhookSecretChecker = new WebhookSecretChecker
  lazy val artifactManager: ArtifactManager = ArtifactManager
  lazy val snsHelper: SnsHelper = SnsHelper
  lazy val lambdaRuntimeProperties: LambdaRuntimeProperties = LambdaRuntimeProperties
  lazy val githubClient = new GitHubClient(lambdaRuntimeProperties.githubPersonalAccessTokens())
  lazy val configReader = new StaticConfigReader()

  val logger = Logger(classOf[Handler])

  //  def createPrototypeBeanstalkApplication_ForInvestigatingSHAIssues(is: InputStream, os: OutputStream, context: Context): Unit = {
  //    println("-" * 20)
  //    println(IOUtils.toString(is, "utf-8"))
  //    println("#" * 20)
  //  }

  def handle(input: Request, context: Context): Response = {
    val regexList: Seq[String] = getRegexList(input)
    LogConfigurator.configureLog4jFromSystemProperties()
    createPrototypeBeanstalkApplication(input, regexList)
  }

  def getRegexList(input: Request): Seq[String] = {
    configReader.readConfig(getConfigName(input))
  }

  def getConfigName(input: Request): String = {
    val queryParameterName = "config"
    input.queryStringParameters
      .asScala
      .getOrElse("config", throw new RuntimeException(s"query parameter '$queryParameterName' not present in the url (${ToStringBuilder.reflectionToString(input)})"))
      .toString
  }

  def createPrototypeBeanstalkApplication(input: Request, regexList: Seq[String]): Response = {

    runAndRespond {
      val payload = input.body
      val payloadDetails = WebHook.getPayloadDetails(payload)
      val idProvider = IdProvider(payloadDetails.commitId)
      logger.info(s"commitId: ${idProvider.get}, event(body): $payload")
      logger.info(s"commitId: ${idProvider.get}, Headers: ${input.headers}")

      val signature = input.headers.get("X-Hub-Signature").toString

      if (webhookSecretChecker.checkSignature(payload, signature)) {

        val tryOfExplodedZipFile: Try[File] = Try(artifactManager.getZipAndExplode(githubClient, payloadDetails, payloadDetails.branchRef))

        tryOfExplodedZipFile match {
          case Success(explodedZipDir) =>
            new RegexMatchingEngine(explodedZipDir,regexList).run

            Response(200, s"""{id: "${idProvider.get}"}""")
          case Failure(t) =>
            Response(500, "Could not download the source code")
        }
      } else {
        val message = "Signature from Github doesn't match what we expect!"
        logger.error(message)
        Response(400, message)
      }
    } match {
      case Right(r) => r
      case Left(t) => Response(400, t.getMessage)
    }
  }

  //  private def getStartCommand(payloadDetails: WebHook.PayloadDetails) = {
  //    val startCommand: String = lambdaRuntimeProperties.getStartCommandFromProcfile match {
  //      case true => {
  //        Handler.extractStartCommandFromProcFile(
  //          githubClient.getFileContent(payloadDetails.contentsUrl,
  //                                      payloadDetails.repositoryOwner,
  //                                      payloadDetails.repositoryName,
  //                                      "Procfile"))
  //      }
  //      case false => Handler.defaultStartCommand
  //    }
  //
  //    logger.info(s"Using start command: $startCommand")
  //    startCommand
  //  }

}

object Handler {

  val defaultStartCommand = "npm start"

  def extractStartCommandFromProcFile(procFileContent: Option[String]): String =
    procFileContent
      .map(_.replaceAll("web:\\s*", ""))
      .getOrElse(defaultStartCommand)
}


class Request(@BeanProperty var body: String,
              @BeanProperty var headers: util.Map[String, Object],
              @BeanProperty var pathParameters: Data,
              @BeanProperty var queryStringParameters: util.Map[String, Object]) {
  def this() = this("", new util.HashMap(), Data(""), new util.HashMap())
}

case class Response(@BeanProperty statusCode: Int, @BeanProperty body: String)

case class Data(@BeanProperty var configFile: String) {
  def this() = this("")
}


////////////
//class GetAllRequest
//
//class CreateRequest(@BeanProperty var body: String) {
//  def this() = this("")
//}
//
//case class Data(@BeanProperty var id: String) {
//  def this() = this("")
//}
//
//class GetOneRequest(@BeanProperty var pathParameters: Data, @BeanProperty var queryStringParameters: util.Map[String, Object]) {
//  def this() = this(Data(""), new util.HashMap())
//
//
//}
//
//class UpdateOneRequest(@BeanProperty var pathParameters: Data, @BeanProperty var body: String) {
//  def this() = this(Data(""),"")
//}
//
//class DeleteRequest(@BeanProperty var pathParameters: Data) {
//  def this() = this(Data(""))
//}

case class Response(@BeanProperty statusCode: Int, @BeanProperty body: String)