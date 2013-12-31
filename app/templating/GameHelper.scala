package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status ⇒ S, Variant, Color, Clock, Mode }
import controllers.routes
import play.api.mvc.Call
import play.api.templates.Html

import lila.game.{ Game, Player, Namer }
import lila.user.Env.{ current ⇒ userEnv }
import lila.user.{ User, Context }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper ⇒
  def variantName(variant: Variant)(implicit ctx: Context) = variant match {
    case Variant.Standard     ⇒ trans.standard.str()
    case Variant.Chess960     ⇒ "chess960"
    case Variant.FromPosition ⇒ trans.fromPosition.str()
  }

  def clockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(trans.unlimited.str())(clockName)

  def clockName(clock: Clock)(implicit ctx: Context): String =
    trans.nbMinutesPerSidePlusNbSecondsPerMove.str(clock.limitInMinutes, clock.increment)

  def clockNameNoCtx(clock: Option[Clock]): String =
    clock.fold(trans.unlimited.en())(clockNameNoCtx)

  def clockNameNoCtx(clock: Clock): String =
    trans.nbMinutesPerSidePlusNbSecondsPerMove.en(clock.limitInMinutes, clock.increment)

  def shortClockName(clock: Option[Clock])(implicit ctx: Context): String =
    clock.fold(trans.unlimited.str())(Namer.shortClock)

  def shortClockName(clock: Clock): String = Namer shortClock clock

  def modeName(mode: Mode)(implicit ctx: Context): String = mode match {
    case Mode.Casual ⇒ trans.casual.str()
    case Mode.Rated  ⇒ trans.rated.str()
  }

  def modeNameNoCtx(mode: Mode): String = mode match {
    case Mode.Casual ⇒ trans.casual.en()
    case Mode.Rated  ⇒ trans.rated.en()
  }

  def playerUsername(player: Player, withRating: Boolean = true) =
    Namer.player(player, withRating)(userEnv.usernameOrAnonymous).await

  def playerLink(
    player: Player,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withRating: Boolean = true,
    withDiff: Boolean = true,
    engine: Boolean = false)(implicit ctx: Context) = Html {
    player.userId.fold(
      """<span class="user_link%s">%s</span>""".format(
        cssClass.??(" " + _),
        player.aiLevel.fold(player.name | User.anonymous)(aiName)
      )
    ) { userId ⇒
        userIdToUsername(userId) |> { username ⇒
          """<a %s href="%s">%s%s</a>""".format(
            userClass(userId, cssClass, withOnline),
            routes.User show username,
            playerUsername(player, withRating) + ~(player.ratingDiff filter (_ ⇒ withDiff) map { diff ⇒
              " (%s)".format(showNumber(diff))
            }),
            engine ?? """<span class="engine_mark" title="%s"></span>""" format trans.thisPlayerUsesChessComputerAssistance()
          )
        }
      }
  }

  def gameEndStatus(game: Game)(implicit ctx: Context): Html = game.status match {
    case S.Aborted ⇒ trans.gameAborted()
    case S.Mate    ⇒ trans.checkmate()
    case S.Resign ⇒ game.loser match {
      case Some(p) if p.color.white ⇒ trans.whiteResigned()
      case _                        ⇒ trans.blackResigned()
    }
    case S.Stalemate ⇒ trans.stalemate()
    case S.Timeout ⇒ game.loser match {
      case Some(p) if p.color.white ⇒ trans.whiteLeftTheGame()
      case _                        ⇒ trans.blackLeftTheGame()
    }
    case S.Draw      ⇒ trans.draw()
    case S.Outoftime ⇒ trans.timeOut()
    case S.Cheat     ⇒ Html("Cheat detected")
    case _           ⇒ Html("")
  }

  def gameFen(game: Game, color: Color, ownerLink: Boolean = false, tv: Boolean = false)(implicit ctx: Context) = Html {
    val owner = ownerLink.fold(ctx.me flatMap game.player, none)
    var live = game.isBeingPlayed
    val url = owner.fold(routes.Round.watcher(game.id, color.name)) { o ⇒
      routes.Round.player(game fullIdOf o.color)
    }
    """<a href="%s" title="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"></a>""".format(
      tv.fold(routes.Tv.index, url),
      trans.viewInFullSize(),
      live ?? ("live live_" + game.id),
      live ?? game.id,
      color.name,
      Forsyth exportBoard game.toChess.board,
      ~game.castleLastMoveTime.lastMoveString)
  }

  def gameFenNoCtx(game: Game, color: Color, tv: Boolean = false) = Html {
    var live = game.isBeingPlayed
    """<a href="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"></a>""".format(
      tv.fold(routes.Tv.index, routes.Round.watcher(game.id, color.name)),
      live ?? ("live live_" + game.id),
      live ?? game.id,
      color.name,
      Forsyth exportBoard game.toChess.board,
      ~game.castleLastMoveTime.lastMoveString)
  }
}
