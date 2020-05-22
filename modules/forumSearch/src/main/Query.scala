package lila.forumSearch

private[forumSearch] case class Query(text: String, troll: Boolean)

object Query {

  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]
}
