package controllers

import play.api.libs.json.Json

import lila.app.*
import lila.setup.SetupBulk

final class BulkPairing(env: Env) extends LilaController(env):

  def list = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk.scheduledBy(me) map { list =>
      JsonOk(Json.obj("bulks" -> list.map(SetupBulk.toJson)))
    }
  }

  def delete(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk.deleteBy(id, me) flatMap {
      if _ then jsonOkResult else notFoundJson()
    }
  }

  def startClocks(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk.startClocks(id, me) flatMap {
      if _ then jsonOkResult else notFoundJson()
    }
  }

  def create = ScopedBody(_.Challenge.Bulk) { ctx ?=> me ?=>
    import lila.setup.SetupBulk
    lila.setup.SetupBulk.form
      .bindFromRequest()
      .fold(
        jsonFormError,
        data =>
          env.setup.bulk(data, me) flatMap {
            case Left(SetupBulk.ScheduleError.RateLimited) =>
              TooManyRequests:
                jsonError(s"Ratelimited! Max games per 10 minutes: ${SetupBulk.maxGames}")
            case Left(SetupBulk.ScheduleError.BadTokens(tokens)) =>
              import lila.setup.SetupBulk.BadToken
              import play.api.libs.json.*
              BadRequest:
                Json.obj:
                  "tokens" -> JsObject:
                    tokens.map:
                      case BadToken(token, error) => token.value -> JsString(error.message)
            case Left(SetupBulk.ScheduleError.DuplicateUsers(users)) =>
              BadRequest(Json.obj("duplicateUsers" -> users))
            case Right(bulk) =>
              env.challenge.bulk.schedule(bulk) map {
                case Left(error) => BadRequest(jsonError(error))
                case Right(bulk) => JsonOk(SetupBulk toJson bulk)
              }
          }
      )
  }
