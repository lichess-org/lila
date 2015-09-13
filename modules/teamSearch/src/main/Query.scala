package lila.teamSearch

private[teamSearch] case class Query(text: String)

object Query {

  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]
}
