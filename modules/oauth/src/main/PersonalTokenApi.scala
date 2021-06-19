package lila.oauth

import reactivemongo.api.bson.BSONObjectID

import lila.db.dsl._
import lila.user.User

final class PersonalTokenApi(colls: OauthColls)(implicit ec: scala.concurrent.ExecutionContext) {

  import PersonalToken._
  import OAuthScope.scopeHandler
  import AccessToken.{ BSONFields => F, _ }

  def list(u: User): Fu[List[AccessToken]] =
    colls.token {
      _.find(
        $doc(
          F.userId   -> u.id,
          F.clientId -> clientId
        )
      )
        .sort($sort desc F.createdAt)
        .cursor[AccessToken]()
        .list(100)
    }

  def findCompatible(u: User, scopes: Set[OAuthScope]): Fu[Option[AccessToken]] =
    colls.token {
      _.one[AccessToken](
        $doc(
          F.userId   -> u.id,
          F.clientId -> clientId,
          F.scopes $all scopes.toSeq
        )
      )
    }

  def create(token: AccessToken) = colls.token(_.insert.one(token).void)

  def deleteByPublicId(publicId: String, user: User): Fu[Option[AccessToken]] =
    BSONObjectID.parse(publicId).toOption ?? { objectId =>
      colls.token { coll =>
        coll
          .one[AccessToken]($doc(F.publicId -> objectId, F.clientId -> clientId, F.userId -> user.id))
          .flatMap {
            _ ?? { token =>
              coll.delete.one($doc(F.publicId -> token.publicId)) inject token.some
            }
          }
      }
    }

  def count(u: User): Fu[Int] =
    colls.token {
      _.countSel($doc(
        F.userId -> u.id,
        F.clientId -> PersonalToken.clientId,
        F.clientOrigin -> $exists(false),
      ))
    }
}

object PersonalToken {

  val clientId = "lichess_personal_token"
}
