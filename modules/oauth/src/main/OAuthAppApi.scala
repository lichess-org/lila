package lila.oauth

import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }

import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthAppApi(coll: AsyncColl) {

  import OAuthApp.{ AppIdHandler, AppBSONHandler }
  import OAuthApp.{ BSONFields => F }

  def list(u: User): Fu[List[OAuthApp]] = coll {
    _.ext.find($doc(F.author -> u.id)).sort($sort desc F.createdAt).list[OAuthApp](30)
  }

  def create(app: OAuthApp) = coll(_.insert.one(app).void)

  def findBy(clientId: OAuthApp.Id, user: User): Fu[Option[OAuthApp]] =
    coll {
      _.one[OAuthApp]($doc(
        F.clientId -> clientId,
        F.author -> user.id
      ))
    }

  def authorOf(clientId: OAuthApp.Id): Fu[Option[User.ID]] =
    coll(_.primitiveOne[User.ID]($doc(F.clientId -> clientId), F.author))

  def update(from: OAuthApp)(f: OAuthApp => OAuthApp): Fu[OAuthApp] = {
    val app = f(from)
    if (app == from) fuccess(app)
    else coll(_.update.one($doc(F.clientId -> app.clientId), app)) inject app
  }

  def deleteBy(clientId: OAuthApp.Id, user: User) = coll {
    _.delete.one($doc(
      F.clientId -> clientId,
      F.author -> user.id
    )).void
  }
}
