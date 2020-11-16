package lila.oauth

import lila.db.dsl._
import lila.user.User

final class PersonalTokenApi(colls: OauthColls)(implicit ec: scala.concurrent.ExecutionContext) {

  import PersonalToken._
  import AccessToken.accessTokenIdHandler
  import AccessToken.{ BSONFields => F, _ }

  def list(u: User): Fu[List[AccessToken]] =
    colls.token {
      _.ext
        .find(
          $doc(
            F.userId   -> u.id,
            F.clientId -> clientId
          )
        )
        .sort($sort desc F.createdAt)
        .list[AccessToken](100)
    }

  def create(token: AccessToken) = colls.token(_.insert.one(token).void)

  def deleteBy(tokenId: AccessToken.Id, user: User) =
    colls.token {
      _.delete
        .one(
          $doc(
            F.id       -> tokenId,
            F.clientId -> clientId,
            F.userId   -> user.id
          )
        )
        .void
    }
}

object PersonalToken {

  val clientId = "lishogi_personal_token"
}
