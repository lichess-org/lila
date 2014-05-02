package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status => S, Variant, Color, Clock, Mode }
import controllers.routes
import play.api.mvc.Call
import play.api.templates.Html

import lila.game.{ Game, Player, Namer }
import lila.user.Env.{ current => userEnv }
import lila.user.{ User, UserContext }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper =>

  def netBaseUrl: String

  def variantName(variant: Variant)(implicit ctx: UserContext) = variant match {
    case Variant.Standard     => trans.standard.str()
    case Variant.Chess960     => "chess960"
    case Variant.FromPosition => trans.fromPosition.str()
  }

  def clockName(clock: Option[Clock])(implicit ctx: UserContext): String =
    clock.fold(trans.unlimited.str())(clockName)

  def clockName(clock: Clock)(implicit ctx: UserContext): String =
    trans.nbMinutesPerSidePlusNbSecondsPerMove.str(clock.limitInMinutes, clock.increment)

  def clockNameNoCtx(clock: Option[Clock]): String =
    clock.fold(trans.unlimited.en())(clockNameNoCtx)

  def clockNameNoCtx(clock: Clock): String =
    trans.nbMinutesPerSidePlusNbSecondsPerMove.en(clock.limitInMinutes, clock.increment)

  def shortClockName(clock: Option[Clock])(implicit ctx: UserContext): Html =
    clock.fold(trans.unlimited())(Namer.shortClock)

  def shortClockName(clock: Clock): Html = Namer shortClock clock

  def modeName(mode: Mode)(implicit ctx: UserContext): String = mode match {
    case Mode.Casual => trans.casual.str()
    case Mode.Rated  => trans.rated.str()
  }

  def modeNameNoCtx(mode: Mode): String = mode match {
    case Mode.Casual => trans.casual.en()
    case Mode.Rated  => trans.rated.en()
  }

  def playerUsername(player: Player, withRating: Boolean = true) =
    Namer.player(player, withRating)(userEnv.lightUser)

  def playerText(player: Player, withRating: Boolean = false) =
    player.aiLevel.fold(
      player.userId.flatMap(userEnv.lightUser).fold("Anon.") { u =>
        player.rating.ifTrue(withRating).fold(u.titleName) { r => s"${u.titleName} ($r)" }
      }
    ) { level => s"A.I. level $level" }

  def playerLink(
    player: Player,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withRating: Boolean = true,
    withDiff: Boolean = true,
    engine: Boolean = false,
    withStatus: Boolean = false)(implicit ctx: UserContext) = Html {
    val statusIcon = if (withStatus) """<span class="status" data-icon="3"></span>""" else ""
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        val content = player.aiLevel.fold(player.name | User.anonymous) { aiName(_, withRating) }
        s"""<span class="user_link$klass">$content$statusIcon</span>"""
      case Some(user) =>
        val klass = userClass(user.id, cssClass, withOnline)
        val href = routes.User show user.name
        val content = playerUsername(player, withRating)
        val diff = (player.ratingDiff ifTrue withDiff).fold(Html(""))(showRatingDiff)
        val mark = engine ?? s"""<span class="engine_mark" title="${trans.thisPlayerUsesChessComputerAssistance()}"></span>"""
        val dataIcon = withOnline ?? """data-icon="r""""
        val space = if (withOnline) "&nbsp;" else ""
        s"""<a $dataIcon $klass href="$href">$space$content$diff$mark$statusIcon</a>"""
    }
  }

  def gameEndStatus(game: Game)(implicit ctx: UserContext): Html = game.status match {
    case S.Aborted => trans.gameAborted()
    case S.Mate    => trans.checkmate()
    case S.Resign => game.loser match {
      case Some(p) if p.color.white => trans.whiteResigned()
      case _                        => trans.blackResigned()
    }
    case S.Stalemate => trans.stalemate()
    case S.Timeout => game.loser match {
      case Some(p) if p.color.white => trans.whiteLeftTheGame()
      case Some(_)                  => trans.blackLeftTheGame()
      case None                     => trans.draw()
    }
    case S.Draw      => trans.draw()
    case S.Outoftime => trans.timeOut()
    case S.Cheat     => Html("Cheat detected")
    case _           => Html("")
  }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c => " • " + c.show }
    s"$u1 vs $u2$clock"
  }

  def gameFen(game: Game, color: Color, ownerLink: Boolean = false, tv: Boolean = false)(implicit ctx: UserContext) = Html {
    val owner = ownerLink.fold(ctx.me flatMap game.player, none)
    var live = game.isBeingPlayed
    val url = owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    }
    """<a href="%s" title="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"></a>""".format(
      tv.fold(routes.Tv.index, url),
      gameTitle(game, color),
      live ?? ("live live_" + game.id),
      live ?? game.id,
      color.name,
      Forsyth exportBoard game.toChess.board,
      ~game.castleLastMoveTime.lastMoveString)
  }

  def gameFenNoCtx(game: Game, color: Color, tv: Boolean = false, blank: Boolean = false) = Html {
    var live = game.isBeingPlayed
    """<a href="%s%s" title="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"%s></a>""".format(
      blank ?? netBaseUrl,
      tv.fold(routes.Tv.index, routes.Round.watcher(game.id, color.name)),
      gameTitle(game, color),
      live ?? ("live live_" + game.id),
      live ?? game.id,
      color.name,
      Forsyth exportBoard game.toChess.board,
      ~game.castleLastMoveTime.lastMoveString,
      blank ?? """ target="_blank"""")
  }
}
