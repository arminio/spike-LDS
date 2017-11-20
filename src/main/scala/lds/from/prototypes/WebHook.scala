package lds.from.prototypes

import play.api.libs.json.{JsLookupResult, Json}

import scala.language.implicitConversions


object WebHook {

  case class PayloadDetails(repositoryName: String,
                            repositoryOwner: String,
                            branchRef: String,
                            repositoryUrl:String,
                            commitId: String,
                            archiveUrl: String,
                            contentsUrl: String)


  implicit def str(js: JsLookupResult): String = {
     js.as[String]
  }

  def getPayloadDetails(payload: String): PayloadDetails = {
    val jsonPayload = Json.parse(payload)

    val ref = jsonPayload \ "ref"
    val repoName = jsonPayload \ "repository" \ "name"
    val repoOwner = jsonPayload \ "repository" \ "owner" \ "name"
    val repoUrl = jsonPayload \ "repository" \ "url"
    val commitId = jsonPayload \ "after"
    val archiveUrl = jsonPayload \ "repository" \ "archive_url"
    val contentsApiUrl = jsonPayload \ "repository" \ "contents_url"

    PayloadDetails(repoName, repoOwner, ref, repoUrl, commitId, archiveUrl, contentsApiUrl)
  }
}
