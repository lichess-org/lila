package lila.studySearch

import play.api.libs.json.OWrites

private[studySearch] case class Query(text: String, userId: Option[String])

object Query {

  implicit val jsonWriter: OWrites[Query] = play.api.libs.json.Json.writes[Query]
}
