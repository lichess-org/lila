package lila.relation

import play.api.libs.json.*

object JsonView:

  given (using Writes[lila.user.User.WithPerfs]): OWrites[Related] with
    def writes(r: Related) = Json.obj(
      "user"       -> r.user,
      "patron"     -> r.user.isPatron,
      "nbGames"    -> r.nbGames,
      "followable" -> r.followable,
      "relation"   -> r.relation
    )
