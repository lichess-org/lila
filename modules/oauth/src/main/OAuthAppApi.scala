package lidraughts.oauth

import play.api.libs.json.Json
import play.api.mvc.{ RequestHeader, Result }

import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo }

final class OAuthAppApi(appColl: Coll) {

  import OAuthApp.{ AppIdHandler, AppBSONHandler }
  import OAuthApp.{ BSONFields => F }

  def list(u: User): Fu[List[OAuthApp]] =
    appColl.find($doc(F.author -> u.id)).sort($sort desc F.createdAt).list[OAuthApp](30)

  def create(app: OAuthApp) = appColl insert app void

  def deleteBy(clientId: OAuthApp.Id, user: User) =
    appColl.remove($doc(
      F.clientId -> clientId,
      F.author -> user.id
    )).void
}
