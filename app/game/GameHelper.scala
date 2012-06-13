package lila
package game

import chess.format.Forsyth
import chess.{ Status, Variant, Color, Clock, Mode }
import user.{ User, UserHelper }
import http.Context
import i18n.I18nHelper
import templating.StringHelper

import controllers.routes

import play.api.templates.Html
import play.api.mvc.Call

trait GameHelper { self: I18nHelper with UserHelper with StringHelper ⇒

  val aiName = "Crafty A.I."

  def variantName(variant: Variant)(implicit ctx: Context) = variant match {
    case Variant.Standard ⇒ trans.standard.str()
    case Variant.Chess960 ⇒ "chess960"
  }

  def clockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(clockName, trans.unlimited.str())

  def clockName(clock: Clock): String = Namer clock clock

  def shortClockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(shortClockName, trans.unlimited.str())

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
      userId ⇒ userIdToUsername(userId) |> { username ⇒
        """<a class="user_link%s%s" href="%s">%s%s</a>""".format(
          cssClass.fold(" " + _, ""),
          withOnline.fold(
            isUsernameOnline(username).fold(" online", " offline"),
            ""),
          routes.User.show(username),
          usernameWithElo(player) + player.eloDiff.filter(_ ⇒ withDiff).fold(
            diff ⇒ " (%s)".format(showNumber(diff)),
            ""),
          engine.fold(
            """<span class="engine_mark tipsyme" title="%s"></span>""" format trans.thisPlayerUsesChessComputerAssistance(),
            "")
        )
      },
      """<span class="user_link %s">%s</span>""".format(
        cssClass | "",
        player.aiLevel.fold(
          trans.aiNameLevelAiLevel(aiName, _),
          User.anonymous)
      )
    )
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
    val url = owner.fold(
      o ⇒ routes.Round.player(game fullIdOf o.color),
      routes.Round.watcher(game.id, color.name)
    )
    """<a href="%s" title="%s" class="mini_board parse_fen" data-color="%s" data-fen="%s"></a>""".format(
      url,
      trans.viewInFullSize(),
      color.name,
      Forsyth exportBoard game.toChess.board)
  }
}
