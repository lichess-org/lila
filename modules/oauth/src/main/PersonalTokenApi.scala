package lila.oauth

import org.joda.time.DateTime

import lila.db.dsl._
import lila.user.User

final class PersonalTokenApi(
    tokenColl: Coll
) {

  import PersonalToken._
  import AccessToken.accessTokenIdHandler
  import AccessToken.{ BSONFields => F, _ }

  def list(u: User): Fu[List[AccessToken]] =
    tokenColl.find($doc(
      F.userId -> u.id,
      F.clientId -> clientId
    )).sort($sort desc F.createdAt).list[AccessToken](100)

  def create(token: AccessToken) = tokenColl insert token void

  def deleteBy(tokenId: AccessToken.Id, user: User) =
    tokenColl.remove($doc(
      F.id -> tokenId,
      F.clientId -> clientId,
      F.userId -> user.id
    )).void
}

object PersonalToken {

  val clientId = "lichess_personal_token"
}
