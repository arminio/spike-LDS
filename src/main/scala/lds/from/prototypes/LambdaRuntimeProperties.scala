package lds.from.prototypes

import com.typesafe.scalalogging.Logger

case class GithubAccessToken(gitHubHost: String, githubPersonalAccessToken: String)

object GithubAccessToken {
  val logger = Logger(classOf[GithubAccessToken])
  def parse(urlsAndPersonalAccessTokens: String): Seq[GithubAccessToken] = {
    def createGithubAccessToken(urlAndToken: String) = {
      urlAndToken.split(":", 2) match {
        case Array(k, v) if k != "" && v != "" =>
          GithubAccessToken(k, v)
        case _ =>
          val errorMessage = "Could not parse GITHUB_PERSONAL_ACCESS_TOKENS, please provide in format: url:token,url:token e.g. github.com:token1,github.somewhere.else:token2"
          logger.error(errorMessage)
          throw new RuntimeException(errorMessage)
      }
    }

    urlsAndPersonalAccessTokens.split(",").map(createGithubAccessToken)
  }
}

trait LambdaRuntimeProperties {
  def webhookSecretKey: String

  def githubPersonalAccessTokens(): Seq[GithubAccessToken]

  def region: String

  def snsTopicName: String

  def prototypeArtifactBucketName: String

  def prototypeEnvironmentName: String

  def iamServiceRole: String

  def instanceType: String

  def imageId: String

  def iamInstanceProfile: String

  def ebVpcId: String

  def ebPublicSubnetIds: String

  def ebPrivateSubnetIds: String

  def getStartCommandFromProcfile: Boolean
}

object LambdaRuntimeProperties extends LambdaRuntimeProperties {

  def webhookSecretKey = getEnvValue("WEBHOOK_SECRET_KEY", None)

  def githubPersonalAccessTokens(): Seq[GithubAccessToken] = GithubAccessToken.parse(getEnvValue("GITHUB_PERSONAL_ACCESS_TOKENS", None))

  def region = getEnvValue("REGION", None, true)
  def snsTopicName = getEnvValue("SNS_TOPIC_NAME", None, true)
  def prototypeArtifactBucketName = getEnvValue("PROTOTYPE_ARTIFACTS_S3_BUCKET", None, true)
  def prototypeEnvironmentName = getEnvValue("PROTOTYPE_ENVIRONMENT_NAME", None, true)

  def iamServiceRole = getEnvValue("BEANSTALK_IAM_SERVICE_ROLE", None, true)
  def beanstalkManagementRoleArn = getEnvValue("BEANSTALK_MGMT_ROLE_ARN", None, true)

  def instanceType = getEnvValue("INSTANCE_TYPE", None, true)
  def imageId = getEnvValue("IMAGE_ID", None, true)

  def iamInstanceProfile = getEnvValue("BEANSTALK_IAM_INSTANCE_PROFILE", None, true)

  def ebVpcId = getEnvValue("EB_VPC_ID", None, true)
  def ebPublicSubnetIds = getEnvValue("EB_PUBLIC_SUBNET_IDS", None, true)
  def ebPrivateSubnetIds = getEnvValue("EB_PRIVATE_SUBNET_IDS", None, true)

  def getStartCommandFromProcfile = getEnvValue("GET_START_COMMAND_FROM_PROCFILE", Some("false"), true).toBoolean

  val logger = Logger(classOf[LambdaRuntimeProperties])

  def getEnvValue(variableName: String, default: Option[String], log: Boolean = false): String = {
    val envValue =
      Option(System.getenv(variableName))
        .getOrElse {
          default.getOrElse {
            val msg = s"FATAL: env variable not found for key '$variableName'"
            logger.error(msg)
            throw new RuntimeException(msg)
          }
    }
    if(log)
      logger.info(s"$variableName: $envValue")
    
    envValue
  }


  import java.nio.ByteBuffer
  import java.nio.charset.Charset

  import com.amazonaws.services.kms.AWSKMSClientBuilder
  import com.amazonaws.services.kms.model.DecryptRequest
  import com.amazonaws.util.Base64

  // this code can be to decrypt the env vars (we don't encrypt any of the env vars any longer as Terraform didn't support this at the moment, perhaps add encryption back in phase 2 again?)
  private def decryptKey(keyToDecrypt: String) = {
    logger.info(s"Decrypting key for $keyToDecrypt")
    val encryptedKey = Base64.decode(getEnvValue(keyToDecrypt, None, false))
    val client = AWSKMSClientBuilder.defaultClient
    val request = new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(encryptedKey))
    val plainTextKey = client.decrypt(request).getPlaintext
    new String(plainTextKey.array, Charset.forName("UTF-8"))
  }

}
