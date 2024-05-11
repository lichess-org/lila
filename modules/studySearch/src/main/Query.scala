package lila.studySearch

import play.api.libs.json.*

import lila.common.Json.given

private[studySearch] case class Query(text: String, userId: Option[UserId]):
  def transform = lila.search.spec.Query.study(text, userId.map(_.value))

given OWrites[Query] = Json.writes
