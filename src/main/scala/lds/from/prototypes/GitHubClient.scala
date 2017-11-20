package lds.from.prototypes

import com.typesafe.scalalogging.Logger

import scalaj.http.{Http, HttpOptions, HttpResponse}

class GitHubClient(gitHubPersonalAccessTokens: Seq[GithubAccessToken]) {

  val logger = Logger(classOf[GitHubClient])

  def getFileContent(contentsUrl: String, organisation: String, repository: String, filePath: String): Option[String] = {
    val githubPersonalAccessToken = selectPersonalApiToken(contentsUrl)
    val filePathInGitHub = s"/$organisation/$repository/$filePath"
    val response: HttpResponse[String] =
      Http(contentsUrl.replace("{+path}", filePath))
        .header("Authorization", s"token $githubPersonalAccessToken")
        .header("Accept", "application/vnd.github.raw")
        .option(HttpOptions.followRedirects(true))
        .asString
    response.code match {
      case 200 => {
        val fileContent = response.body
        logger.info(s"Found file: $filePathInGitHub")
        logger.info(s"File content: $fileContent")
        Some(fileContent)
      }
      case 404 => {
        logger.info(s"File: $filePathInGitHub does not exist")
        None
      }
      case _ => throw new RuntimeException(s"Exception getting file content from Github. Response code: ${response.code}, response body: ${response.body}")
    }
  }

  def selectPersonalApiToken(url : String): String = {
    this.gitHubPersonalAccessTokens.find({
      case GithubAccessToken(githubUrl, _) =>
        logger.debug(s"Identified GitHub personal API token for GitHub URL: $url")
        url.contains(githubUrl)
    }).map(_.githubPersonalAccessToken).getOrElse {
      val errorMessage = s"Could not find suitable GitHub personal API token for Github URL: $url"
      logger.error(errorMessage)
      throw new RuntimeException(errorMessage)
    }
  }

}
