package lila.studySearch

import lila.user.User
import play.api.libs.json.*
import lila.common.Json.given
import lila.study.Order

private[studySearch] case class Query(text: String, userId: Option[UserId])

given OWrites[Query] = Json.writes
