package lila.web

import play.api.libs.json.*
import play.api.mvc.*

import lila.core.game.anonCookieName
import lila.core.id.GamePlayerId
import lila.ui.Context

trait TheftPrevention:
  self: lila.web.ResponseBuilder =>

  protected def PreventTheft(pov: Pov)(ok: => Fu[Result])(using Context): Fu[Result] =
    if isTheft(pov) then Redirect(routes.Round.watcher(pov.gameId, pov.color))
    else ok

  protected def isTheft(pov: Pov)(using ctx: Context) =
    pov.game.isPgnImport || pov.player.isAi || {
      (pov.player.userId, ctx.userId) match
        case (Some(_), None) => true
        case (Some(playerUserId), Some(userId)) => playerUserId != userId
        case (None, _) =>
          lila.common.HTTPRequest.apiVersion(ctx.req).isEmpty &&
          !ctx.req.cookies.get(anonCookieName).exists(_.value == pov.playerId.value)
    }

  protected def isMyPov(pov: Pov)(using Context) = !isTheft(pov)

  protected def playablePovForReq(game: lila.core.game.Game)(using ctx: Context) =
    (!game.isPgnImport && game.playable).so:
      ctx.userId
        .flatMap(game.player)
        .orElse:
          ctx.req.cookies
            .get(anonCookieName)
            .map(c => GamePlayerId(c.value))
            .flatMap(game.playerById)
            .filterNot(_.hasUser)
        .filterNot(_.isAi)
        .map { Pov(game, _) }

  protected lazy val theftResponse = Unauthorized(
    jsonError("This game requires authentication")
  ).as(JSON)
