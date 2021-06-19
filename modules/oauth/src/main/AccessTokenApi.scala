package lila.oauth

import org.joda.time.DateTime
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.user.User

final class AccessTokenApi(colls: OauthColls)(implicit ec: scala.concurrent.ExecutionContext) {

  import OAuthScope.scopeHandler
  import AccessToken.{ BSONFields => F, _ }

  def create(token: AccessToken) = colls.token(_.insert.one(token).void)

  def listPersonal(user: User): Fu[List[AccessToken]] =
    colls.token {
      _.find(
        $doc(
          F.userId       -> user.id,
          F.clientId     -> PersonalToken.clientId,
          F.clientOrigin -> $exists(false)
        )
      )
        .sort($sort desc F.createdAt)
        .cursor[AccessToken]()
        .list(100)
    }

  def countPersonal(user: User): Fu[Int] =
    colls.token {
      _.countSel(
        $doc(
          F.userId       -> user.id,
          F.clientId     -> PersonalToken.clientId,
          F.clientOrigin -> $exists(false)
        )
      )
    }

  def findCompatiblePersonal(user: User, scopes: Set[OAuthScope]): Fu[Option[AccessToken]] =
    colls.token {
      _.one[AccessToken](
        $doc(
          F.userId       -> user.id,
          F.clientId     -> PersonalToken.clientId,
          F.clientOrigin -> $exists(false),
          F.scopes $all scopes.toSeq
        )
      )
    }

  def listClients(user: User, limit: Int): Fu[List[AccessTokenApi.Client]] =
    colls
      .token {
        _.aggregateList(limit) { framework =>
          import framework._
          Match(
            $doc(
              F.userId       -> user.id,
              F.clientOrigin -> $exists(true)
            )
          ) -> List(
            UnwindField(F.scopes),
            GroupField(F.clientOrigin)(
              F.usedAt    -> MaxField(F.usedAt),
              F.scopes    -> AddFieldToSet(F.scopes)
            ),
            Sort(Descending(F.usedAt))
          )
        }
      }
      .map { docs =>
        for {
          doc    <- docs
          origin <- doc.getAsOpt[String]("_id")
          usedAt  = doc.getAsOpt[DateTime](F.usedAt)
          scopes <- doc.getAsOpt[List[OAuthScope]](F.scopes)
        } yield AccessTokenApi.Client(origin, usedAt, scopes)
      }

  def revokeByClientOrigin(clientOrigin: String, user: User): Funit =
    colls.token {
      _.delete
        .one(
          $doc(
            F.userId       -> user.id,
            F.clientOrigin -> clientOrigin
          )
        )
        .void
    }

  def revokeByPublicId(publicId: String, user: User): Fu[Option[AccessToken]] =
    BSONObjectID.parse(publicId).toOption ?? { objectId =>
      colls.token { coll =>
        coll.findAndModify($doc(F.publicId -> objectId, F.userId -> user.id), coll.removeModifier) map {
          _.result[AccessToken]
        }
      }
    }

}

object AccessTokenApi {
  case class Client(
      origin: String,
      usedAt: Option[DateTime],
      scopes: List[OAuthScope]
  )
}

object PersonalToken {

  val clientId = "lichess_personal_token"
}
