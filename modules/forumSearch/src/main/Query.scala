package lila.forumSearch

import play.api.libs.json.*

private case class Query(text: String, troll: Boolean)

given Writes[Query] = Json.writes[Query]
