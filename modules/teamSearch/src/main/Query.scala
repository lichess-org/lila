package lila.teamSearch

import play.api.libs.json.OWrites

private[teamSearch] case class Query(text: String)

object Query {

  implicit val jsonWriter: OWrites[Query] = play.api.libs.json.Json.writes[Query]
}
