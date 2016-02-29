package controllers

import lila.api.Context
import lila.app._
import lila.game.{ Game => GameModel, Pov, AnonCookie }
import lila.security.Granter
import play.api.http.ContentTypes._
import play.api.mvc._
import play.api.mvc.Results._
import views._

private[controllers] trait TheftPrevention { self: LilaController =>

  protected def PreventTheft(pov: Pov)(ok: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    isTheft(pov).fold(fuccess(Redirect(routes.Round.watcher(pov.gameId, pov.color.name))), ok)

  protected def isTheft(pov: Pov)(implicit ctx: Context) = pov.game.isPgnImport || pov.player.isAi || {
    (pov.player.userId, ctx.userId) match {
      case (Some(playerId), None) => true
      case (Some(playerId), Some(userId)) =>
        playerId != userId && !(ctx.me ?? Granter.superAdmin)
      case (None, _) =>
        lila.api.Mobile.Api.requestVersion(ctx.req).isEmpty &&
          !ctx.req.cookies.get(AnonCookie.name).map(_.value).contains(pov.playerId)
    }
  }

  protected def isMyPov(pov: Pov)(implicit ctx: Context) = !isTheft(pov)

  protected def playablePovForReq(game: GameModel)(implicit ctx: Context) =
    (!game.isPgnImport && game.playable) ?? {
      ctx.userId.flatMap(game.playerByUserId).orElse {
        ctx.req.cookies.get(AnonCookie.name).map(_.value)
          .flatMap(game.player).filterNot(_.hasUser)
      }.filterNot(_.isAi).map { Pov(game, _) }
    }

  protected lazy val theftResponse = Unauthorized(jsonError(
    "This game requires authentication"
  )) as JSON
}
