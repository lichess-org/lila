package lila.oauth

import org.joda.time.DateTime

import lila.user.User
import lila.db.dsl._

final class PersonalTokenApi(
    tokenColl: Coll
) {

  import AccessToken.{ BSONFields => F, _ }

  private val clientId = "lichess_personal_token"

  def list(u: User): Fu[List[AccessToken]] =
    tokenColl.find($doc(
      F.userId -> u.id,
      F.clientId -> clientId
    )).sort($sort desc F.createdAt).list[AccessToken](100)
}
