package lila.oauth

import lila.db.dsl._
import lila.user.User

final class OAuthAppApi(appColl: Coll) {

  import OAuthApp.{ AppIdHandler, AppBSONHandler }
  import OAuthApp.{ BSONFields => F }

  def list(u: User): Fu[List[OAuthApp]] =
    appColl.find($doc(F.author -> u.id))
      .sort($sort desc F.createdAt).cursor[OAuthApp]().list(30)

  def create(app: OAuthApp): Funit = appColl.insert.one(app).void

  def findBy(clientId: OAuthApp.Id, user: User): Fu[Option[OAuthApp]] =
    appColl.find($doc(
      F.clientId -> clientId,
      F.author -> user.id
    )).one[OAuthApp]

  def update(from: OAuthApp)(f: OAuthApp => OAuthApp): Fu[OAuthApp] = {
    val app = f(from)
    if (app == from) fuccess(app)
    else appColl.update.one(
      q = $doc(F.clientId -> app.clientId), u = app
    ) inject app
  }

  def deleteBy(clientId: OAuthApp.Id, user: User): Funit =
    appColl.delete.one($doc(
      F.clientId -> clientId,
      F.author -> user.id
    )).void
}
