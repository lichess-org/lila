package lila.forumSearch

import play.api.libs.json.Json
import play.api.libs.json.OWrites

private[forumSearch] case class Query(text: String, troll: Boolean)

object Query {

  implicit val jsonWriter: OWrites[Query] = Json.writes[Query]
}
