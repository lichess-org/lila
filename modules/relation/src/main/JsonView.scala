package lila.relation

import play.api.libs.json.*

import lila.common.Json.given

object JsonView:

  given [U](using Writes[U]): OWrites[Related[U]] with
    def writes(r: Related[U]) = Json.obj(
      "user"       -> r.user,
      "nbGames"    -> r.nbGames,
      "followable" -> r.followable,
      "relation"   -> r.relation
    )
