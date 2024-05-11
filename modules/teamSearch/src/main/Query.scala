package lila.teamSearch

import play.api.libs.json.*

private case class Query(text: String):
  def transform = lila.search.spec.Query.team(text)

private given Writes[Query] = Json.writes[Query]
