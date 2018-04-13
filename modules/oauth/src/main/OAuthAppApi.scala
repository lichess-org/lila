package lila.oauth

import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class OAuthAppApi(appColl: Coll) {

  import OAuthApp.{ AppIdHandler, AppBSONHandler }
  import OAuthApp.{ BSONFields => F }

  def list(u: User): Fu[List[OAuthApp]] =
    appColl.find($doc(F.author -> u.id)).sort($sort desc F.createdAt).list[OAuthApp](30)

  def create(app: OAuthApp) = appColl insert app void

  def findBy(clientId: OAuthApp.Id, user: User): Fu[Option[OAuthApp]] =
    appColl.uno[OAuthApp]($doc(
      F.clientId -> clientId,
      F.author -> user.id
    ))

  def update(from: OAuthApp)(f: OAuthApp => OAuthApp): Fu[OAuthApp] = {
    val app = f(from)
    if (app == from) fuccess(app)
    else appColl.update($doc(F.clientId -> app.clientId), app) inject app
  }

  def deleteBy(clientId: OAuthApp.Id, user: User) =
    appColl.remove($doc(
      F.clientId -> clientId,
      F.author -> user.id
    )).void
}
