package lila
package game

import chess.format.Forsyth
import chess.{ Status, Variant, Color, Clock, Mode }
import user.{ User, UserHelper }
import http.Context
import i18n.I18nHelper
import ai.AiHelper
import templating.StringHelper

import controllers.routes

import play.api.templates.Html
import play.api.mvc.Call

trait GameHelper { self: I18nHelper with UserHelper with StringHelper with AiHelper ⇒

  def variantName(variant: Variant)(implicit ctx: Context) = variant match {
    case Variant.Standard ⇒ trans.standard.str()
    case Variant.Chess960 ⇒ "chess960"
  }

  def clockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(trans.unlimited.str())(Namer.clock)

  def clockName(clock: Clock): String = Namer clock clock

  def shortClockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(trans.unlimited.str())(Namer.shortClock)

  def shortClockName(clock: Clock): String = Namer shortClock clock

  def modeName(mode: Mode)(implicit ctx: Context): String = mode match {
    case Mode.Casual ⇒ trans.casual.str()
    case Mode.Rated  ⇒ trans.rated.str()
  }

  def usernameWithElo(player: DbPlayer) = Namer.player(player)(userIdToUsername)

  def playerLink(
    player: DbPlayer,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withDiff: Boolean = true,
    engine: Boolean = false)(implicit ctx: Context) = Html {
    player.userId.fold(
      """<span class="user_link %s">%s</span>""".format(
        cssClass | "",
        player.aiLevel.fold(User.anonymous)(aiName)
      )
    ) { userId ⇒
        userIdToUsername(userId) |> { username ⇒
          """<a class="user_link%s%s" href="%s">%s%s</a>""".format(
            ~cssClass.map(" " + _),
            withOnline.fold(
              isUsernameOnline(username).fold(" online", " offline"),
              ""),
            routes.User.show(username),
            usernameWithElo(player) + ~(player.eloDiff filter (_ ⇒ withDiff) map { diff ⇒
              " (%s)".format(showNumber(diff))
            }),
            engine.fold(
              """<span class="engine_mark" title="%s"></span>""" format trans.thisPlayerUsesChessComputerAssistance(),
              "")
          )
        }
      }
  }

  def gameEndStatus(game: DbGame)(implicit ctx: Context): Html = game.status match {
    case Status.Aborted ⇒ trans.gameAborted()
    case Status.Mate    ⇒ trans.checkmate()
    case Status.Resign ⇒ game.loser match {
      case Some(p) if p.color.white ⇒ trans.whiteResigned()
      case _                        ⇒ trans.blackResigned()
    }
    case Status.Stalemate ⇒ trans.stalemate()
    case Status.Timeout ⇒ game.loser match {
      case Some(p) if p.color.white ⇒ trans.whiteLeftTheGame()
      case _                        ⇒ trans.blackLeftTheGame()
    }
    case Status.Draw      ⇒ trans.draw()
    case Status.Outoftime ⇒ trans.timeOut()
    case Status.Cheat     ⇒ Html("Cheat detected")
    case _                ⇒ Html("")
  }

  def gameFen(game: DbGame, color: Color, ownerLink: Boolean = false)(implicit ctx: Context) = Html {
    val owner = ownerLink.fold(ctx.me flatMap game.player, none)
    var live = game.isBeingPlayed
    val url = owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    }
    """<a href="%s" title="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"></a>""".format(
      url,
      trans.viewInFullSize(),
      live.fold("live live_" + game.id, ""),
      live.fold(game.id, ""),
      color.name,
      Forsyth exportBoard game.toChess.board,
      game.lastMove | "")
  }

  def gameFenNoCtx(game: DbGame, color: Color) = Html {
    var live = game.isBeingPlayed
    """<a href="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"></a>""".format(
      routes.Round.watcher(game.id, color.name),
      live.fold("live live_" + game.id, ""),
      live.fold(game.id, ""),
      color.name,
      Forsyth exportBoard game.toChess.board,
      game.lastMove | "")
  }
}
