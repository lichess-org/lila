package controllers

import play.api.libs.json.Json

import lila.app._
import lila.setup.SetupBulk

final class BulkPairing(env: Env) extends LilaController(env) {

  def list =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      env.challenge.bulk.scheduledBy(me) map { list =>
        Ok(Json.obj("bulks" -> list.map(SetupBulk.toJson))) as JSON
      }
    }

  def delete(id: String) =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      env.challenge.bulk.deleteBy(id, me) flatMap {
        case true => jsonOkResult.fuccess
        case _    => notFoundJson()
      }
    }

  def create =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      implicit val lang = reqLang
      import lila.setup.SetupBulk
      lila.setup.SetupBulk.form
        .bindFromRequest()
        .fold(
          newJsonFormError,
          data =>
            env.setup.bulk(data, me) flatMap {
              case Left(SetupBulk.RateLimited) =>
                TooManyRequests(
                  jsonError(s"Ratelimited! Max games per 10 minutes: ${SetupBulk.maxGames}")
                ).fuccess
              case Left(SetupBulk.BadTokens(tokens)) =>
                import lila.setup.SetupBulk.BadToken
                import play.api.libs.json._
                BadRequest(
                  Json.obj(
                    "tokens" -> JsObject {
                      tokens.map { case BadToken(token, error) =>
                        token.value -> JsString(error.message)
                      }
                    }
                  )
                ).fuccess
              case Left(SetupBulk.DuplicateUsers(users)) =>
                BadRequest(Json.obj("duplicateUsers" -> users)).fuccess
              case Right(bulk) =>
                env.challenge.bulk.schedule(bulk) map {
                  case Left(error) => BadRequest(jsonError(error))
                  case Right(bulk) => Ok(SetupBulk toJson bulk) as JSON
                }
            }
        )
    }
}
