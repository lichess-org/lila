package lila.oauth

import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User

final class PersonalTokenApi(coll: AsyncColl) {

  import PersonalToken._
  import AccessToken.accessTokenIdHandler
  import AccessToken.{ BSONFields => F, _ }

  def list(u: User): Fu[List[AccessToken]] = coll {
    _.ext.find($doc(
      F.userId -> u.id,
      F.clientId -> clientId
    )).sort($sort desc F.createdAt).list[AccessToken](100)
  }

  def create(token: AccessToken) = coll(_.insert.one(token).void)

  def deleteBy(tokenId: AccessToken.Id, user: User) = coll {
    _.delete.one($doc(
      F.id -> tokenId,
      F.clientId -> clientId,
      F.userId -> user.id
    )).void
  }
}

object PersonalToken {

  val clientId = "lichess_personal_token"
}
