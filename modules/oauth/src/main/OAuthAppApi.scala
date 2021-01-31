package lila.oauth

import lila.db.dsl._
import lila.user.User

final class OAuthAppApi(colls: OauthColls)(implicit ec: scala.concurrent.ExecutionContext) {

  import OAuthApp.{ AppBSONHandler, AppIdHandler }
  import OAuthApp.{ BSONFields => F }

  def mine(u: User): Fu[List[OAuthApp]] =
    colls.app {
      _.find($doc(F.author -> u.id)).sort($sort desc F.createdAt).cursor[OAuthApp]().list(30)
    }

  def create(app: OAuthApp) = colls.app(_.insert.one(app).void)

  def findBy(clientId: OAuthApp.Id, user: User): Fu[Option[OAuthApp]] =
    colls.app {
      _.one[OAuthApp](
        $doc(
          F.clientId -> clientId,
          F.author   -> user.id
        )
      )
    }

  def authorizedBy(user: User): Fu[List[AccessToken.WithApp]] =
    colls.app { appColl =>
      import OAuthApp.AppBSONHandler
      colls.token {
        _.aggregateList(maxDocs = 100) { implicit framework =>
          import framework._
          Match($doc("user_id" -> user.id)) -> List(
            Sort(Descending("used_at")),
            PipelineOperator(
              $doc(
                "$lookup" -> $doc(
                  "from"         -> appColl.name,
                  "localField"   -> "client_id",
                  "foreignField" -> "client_id",
                  "as"           -> "app"
                )
              )
            )
          )
        }.map { docs =>
          for {
            doc   <- docs
            token <- AccessToken.AccessTokenBSONHandler.readOpt(doc)
            app   <- doc.getAsOpt[List[OAuthApp]]("app").??(_.headOption)
          } yield AccessToken.WithApp(token, app)
        }
      }
    }

  def revoke(id: AccessToken.Id, user: User): Funit =
    colls.token {
      _.delete.one($doc("access_token_id" -> id, "user_id" -> user.id)).void
    }

  def authorOf(clientId: OAuthApp.Id): Fu[Option[User.ID]] =
    colls.app(_.primitiveOne[User.ID]($doc(F.clientId -> clientId), F.author))

  def update(from: OAuthApp)(f: OAuthApp => OAuthApp): Fu[OAuthApp] = {
    val app = f(from)
    if (app == from) fuccess(app)
    else colls.app(_.update.one($doc(F.clientId -> app.clientId), app)) inject app
  }

  def deleteBy(clientId: OAuthApp.Id, user: User) =
    colls.app {
      _.delete
        .one(
          $doc(
            F.clientId -> clientId,
            F.author   -> user.id
          )
        )
        .void
    }
}
