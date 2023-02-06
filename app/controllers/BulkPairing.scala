package controllers

import play.api.libs.json.Json

import lila.app.{ given, * }
import lila.setup.SetupBulk

final class BulkPairing(env: Env) extends LilaController(env):

  def list =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      env.challenge.bulk.scheduledBy(me) map { list =>
        JsonOk(Json.obj("bulks" -> list.map(SetupBulk.toJson)))
      }
    }

  def delete(id: String) =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      env.challenge.bulk.deleteBy(id, me) flatMap {
        case true => jsonOkResult.toFuccess
        case _    => notFoundJson()
      }
    }

  def startClocks(id: String) =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      env.challenge.bulk.startClocks(id, me) flatMap {
        case true => jsonOkResult.toFuccess
        case _    => notFoundJson()
      }
    }

  def create =
    ScopedBody(_.Challenge.Bulk) { implicit req => me =>
      given play.api.i18n.Lang = reqLang
      import lila.setup.SetupBulk
      lila.setup.SetupBulk.form
        .bindFromRequest()
        .fold(
          newJsonFormError,
          data =>
            env.setup.bulk(data, me) flatMap {
              case Left(SetupBulk.ScheduleError.RateLimited) =>
                TooManyRequests(
                  jsonError(s"Ratelimited! Max games per 10 minutes: ${SetupBulk.maxGames}")
                ).toFuccess
              case Left(SetupBulk.ScheduleError.BadTokens(tokens)) =>
                import lila.setup.SetupBulk.BadToken
                import play.api.libs.json.*
                BadRequest(
                  Json.obj(
                    "tokens" -> JsObject {
                      tokens.map { case BadToken(token, error) =>
                        token.value -> JsString(error.message)
                      }
                    }
                  )
                ).toFuccess
              case Left(SetupBulk.ScheduleError.DuplicateUsers(users)) =>
                BadRequest(Json.obj("duplicateUsers" -> users)).toFuccess
              case Right(bulk) =>
                env.challenge.bulk.schedule(bulk) map {
                  case Left(error) => BadRequest(jsonError(error))
                  case Right(bulk) => JsonOk(SetupBulk toJson bulk)
                }
            }
        )
    }
