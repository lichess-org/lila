package lidraughts.forumSearch

private[forumSearch] case class Query(text: String, staff: Boolean, troll: Boolean)

object Query {

  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]
}
