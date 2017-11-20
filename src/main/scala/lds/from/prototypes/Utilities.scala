package lds.from.prototypes

import awscala.DefaultCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder
import com.typesafe.scalalogging.Logger
import play.api.libs.json.Json

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.{Failure, Success, Try}

//object DifferenceEngine {
//
//  val logger = Logger[this.type]
//
//  def getDiff(currentConfigurations: Seq[ConfigurationOptionSetting],
//              newConfigurations: Seq[ConfigurationOptionSetting]): Seq[ConfigurationOptionSetting] = {
//    logger.info("Comparing configuration changes:")
//    logger.info(s"current conf: ${currentConfigurations.toList}")
//    logger.info(s"new conf: $newConfigurations")
//
//    val diff = newConfigurations.foldLeft(Seq.empty[ConfigurationOptionSetting]) { (acc, newConf) =>
//      currentConfigurations.find { currentConf =>
//        currentConf.getNamespace == newConf.getNamespace && currentConf.getOptionName == newConf.getOptionName && currentConf.getValue == newConf.getValue
//      }.fold(acc :+ newConf)(_ => acc)
//    }
//
//    logger.info(s"Diff: $diff")
//    diff
//  }
//
//}


//trait ElasticBeanstalkHelper {
//
//  val ebClient: AWSElasticBeanstalk
//  val logger = Logger(classOf[ElasticBeanstalkHelper])
//
//  def getAllEnvironmentNamesAndEndpointURLs(prototypeEnv: String): Seq[EbState] = {
//    val environmentDescriptions =
//      ebClient
//        .describeEnvironments
//        .getEnvironments.asScala
//      .filter(_.getEnvironmentName.startsWith(s"$prototypeEnv-"))
//
//    logger.info(s"$prototypeEnv environment descriptions: $environmentDescriptions")
//
//    getReadyEnvironments(environmentDescriptions)
//  }
//
//  val credentialsProvider: DefaultCredentialsProvider
//  val region: String
//
//  private def getReadyEnvironments(environmentDescriptions: mutable.Buffer[EnvironmentDescription]): Seq[EbState] = {
//    val (readyEnvs, nonReadyEnvs) = environmentDescriptions.partition(_.getStatus.toLowerCase == "ready")
//
//    readyEnvs
//      .map(environmentDescription => (environmentDescription.getApplicationName, environmentDescription.getEndpointURL))
//      .map { case (appName, url) =>
//        if (url.startsWith("http://") || url.startsWith("https://"))
//          EbState(appName, Right(url))
//        else
//          EbState(appName, Right(s"http://$url"))
//      } ++
//      nonReadyEnvs
//        .map(environmentDescription =>
//          EbState(environmentDescription.getApplicationName, Left(environmentDescription.getStatus)))
//  }
//
//
//}

//object ElasticBeanstalkHelper extends ElasticBeanstalkHelper {
//  override val credentialsProvider: DefaultCredentialsProvider = DefaultCredentialsProvider()
//  override val region: String = LambdaRuntimeProperties.region.toLowerCase
//
//  override val ebClient: AWSElasticBeanstalk = AWSElasticBeanstalkClientBuilder.standard()
//    .withCredentials(credentialsProvider)
//    .withEndpointConfiguration(new EndpointConfiguration(s"elasticbeanstalk.$region.amazonaws.com", Regions.fromName(region).getName))
//    .build()
//}


case class DeployCommand(prototypeAppName: String,
                         prototypeEnvName: String,
                         commitId: String,
                         s3ZipArtefactKey: String,
                         retries: Int = 0)

object DeployCommand {
  implicit val deployCommandFormat = Json.format[DeployCommand]
}



trait SnsHelper {
  val credentialsProvider : DefaultCredentialsProvider
  def publishDeploymentRequest(deployCommand: DeployCommand): Unit

    //  val region: String
//  val topicName: String
//  val snsClient: AmazonSNSAsync
}

object SnsHelper extends SnsHelper {

  val logger = Logger[this.type]

  val credentialsProvider = DefaultCredentialsProvider()

  def publishDeploymentRequest(deployCommand: DeployCommand): Unit = {

    import scala.language.postfixOps

    val region = LambdaRuntimeProperties.region.toLowerCase
    val topicName = LambdaRuntimeProperties.snsTopicName

    val snsClient = AmazonSNSAsyncClientBuilder.standard()
      .withCredentials(credentialsProvider)
      .withEndpointConfiguration(new EndpointConfiguration(s"sns.${region}.amazonaws.com", Regions.fromName(region).getName)).build()

    val ts = snsClient.listTopics()
    val maybeTopic = ts.getTopics.asScala.find(_.getTopicArn.contains(topicName))
    logger.info(maybeTopic.toString())

    maybeTopic.fold(throw new RuntimeException(s"No topic with name $topicName found")) { tpc =>
      val deployCommandStr = Json.stringify(Json.toJson(deployCommand))
      logger.info(s"Dispatching :$deployCommandStr")
      snsClient.publish(tpc.getTopicArn, deployCommandStr)
    }
  }

}

object MiscHelper {

  val logger = Logger[this.type]

  private def normalizeAppName(repositoryName: String): String =
    repositoryName.replaceAll("_", "").replaceAll(" ", "").replaceAll("-", "")

  /**
    * AWS imposes a maximum length of 40 characters for Elastic Beanstalk environment names.
    * 40 characters should still give us enough uniqueness whilst still being easily human readable.
    */
  def getEnvName(repositoryName: String, prototypeEnvironment: String): String = {

    val environmentName = s"$prototypeEnvironment-${normalizeAppName(repositoryName)}"
    environmentName match {
      case _ if environmentName.length > 40 => {
        val truncatedEnvironmentName = environmentName.substring(0, 40)
        logger.info(s"Environment name: '$environmentName' is longer than 40 characters. Truncating to '$truncatedEnvironmentName' to meet AWS restrictions")
        truncatedEnvironmentName
      }
      case _ => environmentName
    }
  }


  def runAndRespond[T](f: => T): Either[Throwable, T] = {
    Try(f) match {
      case Success(r) => Right(r)
      case Failure(t) =>
        logger.error("Error Occurred:", t)
        Left(t)
    }
  }

}

