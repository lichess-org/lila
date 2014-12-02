package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status => S, Variant, Color, Clock, Mode }
import controllers.routes
import play.api.mvc.Call
import play.twirl.api.Html

import lila.game.{ Game, Player, Namer, Pov }
import lila.user.{ User, UserContext }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper =>

  def netBaseUrl: String
  def staticUrl(path: String): String
  def cdnUrl(path: String): String

  def mandatorySecondsToMove = lila.game.Env.current.MandatorySecondsToMove

  def povOpenGraph(pov: Pov) = {
    val speed = chess.Speed(pov.game.clock).name
    val variant = pov.game.variant.exotic ?? s" ${pov.game.variant.name}"
    Map(
      'type -> "website",
      'image -> cdnUrl(routes.Export.png(pov.game.id).url),
      'title -> s"$speed$variant Chess - ${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}",
      'site_name -> "lichess.org",
      'url -> s"$netBaseUrl${routes.Round.watcher(pov.game.id, pov.color.name).url}",
      'description -> describePov(pov))
  }

  def describePov(pov: Pov) = {
    import pov._
    val p1 = playerText(player, withRating = true)
    val p2 = playerText(opponent, withRating = true)
    val speedAndClock = game.clock.fold(chess.Speed.Unlimited.name) { c =>
      s"${chess.Speed(c.some).name} (${c.show})"
    }
    val mode = game.mode.name
    val variant = if (game.variant == chess.Variant.FromPosition) "position setup chess"
    else if (game.variant.exotic) game.variant.name else "chess"
    import chess.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} forfeits by time"
      case (_, _, Draw | Stalemate)                         => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case (_, _, VariantEnd) => game.variant match {
        case Variant.KingOfTheHill => "King in the center"
        case Variant.ThreeCheck    => "Three checks"
        case _                     => "Variant ending"
      }
      case _ => "Game is still being played"
    }
    val moves = s"${game.turns} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: Variant)(implicit ctx: UserContext) = variant match {
    case Variant.Standard     => trans.standard.str()
    case Variant.FromPosition => trans.fromPosition.str()
    case v                    => v.name
  }

  def variantNameNoCtx(variant: Variant) = variant match {
    case Variant.Standard     => trans.standard.en()
    case Variant.FromPosition => trans.fromPosition.en()
    case v                    => v.name
  }

  def shortClockName(clock: Option[Clock])(implicit ctx: UserContext): Html =
    clock.fold(trans.unlimited())(shortClockName)

  def shortClockName(clock: Clock): Html = Html(clock.show)

  def modeName(mode: Mode)(implicit ctx: UserContext): String = mode match {
    case Mode.Casual => trans.casual.str()
    case Mode.Rated  => trans.rated.str()
  }

  def modeNameNoCtx(mode: Mode): String = mode match {
    case Mode.Casual => trans.casual.en()
    case Mode.Rated  => trans.rated.en()
  }

  def playerUsername(player: Player, withRating: Boolean = true, withTitle: Boolean = true) =
    Namer.player(player, withRating, withTitle)(lightUser)

  def playerText(player: Player, withRating: Boolean = false) =
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold(player.name | "Anon.") { u =>
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
    withStatus: Boolean = false,
    mod: Boolean = false)(implicit ctx: UserContext) = Html {
    val statusIcon = if (withStatus) """<span class="status"></span>""" else ""
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        val content = player.aiLevel.fold(player.name | User.anonymous) { aiName(_, withRating) }
        s"""<span class="user_link$klass">$content$statusIcon</span>"""
      case Some(user) =>
        val klass = userClass(user.id, cssClass, withOnline)
        val href = s"${routes.User show user.name}${if (mod) "?mod" else ""}"
        val content = playerUsername(player, withRating)
        val diff = (player.ratingDiff ifTrue withDiff).fold(Html(""))(showRatingDiff)
        val mark = engine ?? s"""<span class="engine_mark" title="${trans.thisPlayerUsesChessComputerAssistance()}"></span>"""
        val dataIcon = withOnline ?? """data-icon="r""""
        val space = if (withOnline) "&nbsp;" else ""
        s"""<a $dataIcon $klass href="$href">$space$content$diff$mark</a>$statusIcon"""
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
    case S.NoStart => Html {
      val color = game.loser.fold(Color.white)(_.color).name.capitalize
      s"$color didn't move"
    }
    case S.Cheat => Html("Cheat detected")
    case S.VariantEnd => game.variant match {
      case Variant.KingOfTheHill => Html("King in the center")
      case Variant.ThreeCheck    => Html("Three checks")
      case _                     => Html("Variant ending")
    }
    case _ => Html("")
  }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c => " • " + c.show }
    val variant = game.variant.exotic ?? s" • ${game.variant.name}"
    s"$u1 vs $u2$clock$variant"
  }

  // whiteUsername 1-0 blackUsername
  def gameSummary(whiteUserId: String, blackUserId: String, finished: Boolean, result: Option[Boolean]) = {
    val res = if (finished) result match {
      case Some(true)  => "1-0"
      case Some(false) => "0-1"
      case None        => "½-½"
    }
    else "*"
    s"${usernameOrId(whiteUserId)} $res ${usernameOrId(blackUserId)}"
  }

  lazy val miniBoardContent = Html("""<div class="cg-board-wrap"><div class="cg-board"></div></div>""")

  def gameFen(
    game: Game,
    color: Color,
    ownerLink: Boolean = false,
    tv: Boolean = false,
    withTitle: Boolean = true,
    withLink: Boolean = true)(implicit ctx: UserContext) = Html {
    var isLive = game.isBeingPlayed
    val href = withLink ?? {
      val owner = ownerLink.fold(ctx.me flatMap game.player, none)
      val url = tv.fold(routes.Tv.index, owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
        routes.Round.player(game fullIdOf o.color)
      })
      s"""href="$url""""
    }
    val title = withTitle ?? s"""title="${gameTitle(game, color)}""""
    val cssClass = isLive ?? ("live live_" + game.id)
    val live = isLive ?? game.id
    val fen = Forsyth exportBoard game.toChess.board
    val lastMove = ~game.castleLastMoveTime.lastMoveString
    val variant = game.variant.key
    val tag = if (withLink) "a" else "span"
    s"""<$tag $href $title class="mini_board parse_fen $cssClass $variant" data-live="$live" data-color="${color.name}" data-fen="$fen" data-lastmove="$lastMove">$miniBoardContent</$tag>"""
  }

  def gameFenNoCtx(game: Game, color: Color, tv: Boolean = false, blank: Boolean = false) = Html {
    var isLive = game.isBeingPlayed
    val variant = game.variant.key
    s"""<a href="%s%s" title="%s" class="mini_board parse_fen %s $variant" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"%s>$miniBoardContent</a>""".format(
      blank ?? netBaseUrl,
      tv.fold(routes.Tv.index, routes.Round.watcher(game.id, color.name)),
      gameTitle(game, color),
      isLive ?? ("live live_" + game.id),
      isLive ?? game.id,
      color.name,
      Forsyth exportBoard game.toChess.board,
      ~game.castleLastMoveTime.lastMoveString,
      blank ?? """ target="_blank"""")
  }
}
