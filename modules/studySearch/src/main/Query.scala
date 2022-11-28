package lila.studySearch

import lila.user.User
import play.api.libs.json.*

private[studySearch] case class Query(text: String, userId: Option[UserId])

given OWrites[Query] = Json.writes
