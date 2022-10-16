package lila.studySearch

import lila.user.User

private[studySearch] case class Query(text: String, userId: Option[User.ID])

object Query {

  implicit val jsonWriter = play.api.libs.json.Json.writes[Query]
}
