package lila.studySearch

import play.api.libs.json.*
import lila.common.Json.given

private[studySearch] case class Query(text: String, userId: Option[UserId])

given OWrites[Query] = Json.writes
