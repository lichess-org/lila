package lila.teamSearch

import play.api.libs.json.*

private case class Query(text: String)

private given Writes[Query] = Json.writes[Query]
