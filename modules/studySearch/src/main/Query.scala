package lila.studySearch

private[studySearch] case class Query(text: String, userId: Option[String])

object Query {

  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]
}
