package lila.relation

import play.api.libs.json._

case class Related(
    user: lila.user.User,
    nbGames: Int,
    followable: Boolean,
    relation: Option[Relation]) {

  def toJson(implicit userWrites: Writes[lila.user.User]) = Json.obj(
    "user" -> user,
    "nbGames" -> nbGames,
    "followable" -> followable,
    "relation" -> relation)
}
