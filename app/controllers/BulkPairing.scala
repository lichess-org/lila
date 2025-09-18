package controllers

import play.api.libs.json.*

import lila.api.GameApiV2
import lila.app.*
import lila.challenge.ChallengeBulkSetup
import lila.common.Json.given

final class BulkPairing(gameC: => Game, apiC: => Api, env: Env) extends LilaController(env):

  def list = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .scheduledBy(me)
      .map: list =>
        JsonOk(Json.obj("bulks" -> list.map(ChallengeBulkSetup.toJson)))
  }

  def show(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .findBy(id, me)
      .map:
        _.fold(notFoundJson()): bulk =>
          JsonOk(ChallengeBulkSetup.toJson(bulk))
  }

  def games(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .findBy(id, me)
      .map:
        _.fold(notFoundText()): bulk =>
          val config = GameApiV2.ByIdsConfig(
            ids = bulk.games.map(_.id),
            format = GameApiV2.Format.byRequest,
            flags = gameC.requestPgnFlags(extended = false).copy(delayMoves = false),
            perSecond = MaxPerSecond(50)
          )
          apiC.GlobalConcurrencyLimitPerIP
            .download(req.ipAddress)(env.api.gameApiV2.exportByIds(config)): source =>
              Ok.chunked(source).as(gameC.gameContentType(config)).noProxyBuffer
  }

  def delete(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .deleteBy(id, me)
      .flatMap:
        if _ then jsonOkResult else notFoundJson()
  }

  def startClocks(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .startClocksAsap(id, me)
      .flatMap:
        if _ then jsonOkResult else notFoundJson()
  }

  def create = ScopedBody(_.Challenge.Bulk) { ctx ?=> me ?=>
    bindForm(env.challenge.bulkSetup.form)(
      jsonFormError,
      data =>
        import ChallengeBulkSetup.*
        env.challenge
          .bulkSetupApi(data, me)
          .flatMap:
            case Left(ScheduleError.RateLimited) =>
              TooManyRequests:
                jsonError(s"Ratelimited! Max games per 10 minutes: ${maxGames}")
            case Left(ScheduleError.BadTokens(tokens)) =>
              BadRequest:
                Json.obj:
                  "tokens" -> JsObject:
                    tokens.map:
                      case BadToken(token, error) => token.value -> JsString(error.message)
            case Left(ScheduleError.DuplicateUsers(users)) =>
              BadRequest(Json.obj("duplicateUsers" -> users))
            case Right(bulk) =>
              env.challenge.bulk
                .schedule(bulk)
                .map:
                  case Left(error) => BadRequest(jsonError(error))
                  case Right(bulk) => JsonOk(toJson(bulk))
    )
  }
