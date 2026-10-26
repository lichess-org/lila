package controllers

import cats.mtl.Handle.*
import play.api.libs.json.*

import lila.api.GameApiV2
import lila.app.*
import lila.challenge.ChallengeBulkSetup
import lila.common.Json.given

final class BulkPairing(gameC: => Game, env: Env) extends LilaController(env):

  def list = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .scheduledBy(me)
      .map: list =>
        Ok(Json.obj("bulks" -> list.map(ChallengeBulkSetup.toJson)))
  }

  def show(id: String) = ScopedBody(_.Challenge.Bulk) { _ ?=> me ?=>
    env.challenge.bulk
      .findBy(id, me)
      .map:
        _.fold(notFoundJson()): bulk =>
          Ok(ChallengeBulkSetup.toJson(bulk))
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
          limit.GlobalConcurrencyLimitPerIP
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
        allow:
          for
            bulk <- env.challenge.bulkSetupApi(data)
            scheduled <- env.challenge.bulk.schedule(bulk)
          yield Ok(toJson(scheduled))
        .rescue:
          case ScheduleError.RateLimited =>
            TooManyRequests:
              jsonError(s"Ratelimited! Max games per 10 minutes: ${maxGamesPerTenMinutes}")
          case ScheduleError.BadTokens(tokens) =>
            BadRequest:
              Json.obj:
                "tokens" -> JsObject:
                  tokens.map: t =>
                    t.token.value -> JsString(t.error.message)
          case ScheduleError.DuplicateUsers(users) =>
            BadRequest(Json.obj("duplicateUsers" -> users))
          case error: String => BadRequest(jsonError(error))
    )
  }
