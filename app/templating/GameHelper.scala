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

  def mandatorySecondsToMove = lila.game.Env.current.MandatorySecondsToMove

  def povOpenGraph(pov: Pov) = Map(
    'type -> "website",
    'image -> staticUrl("images/large_tile.png"),
    'title -> s"${chess.Speed(pov.game.clock).toString} Chess - ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
    'site_name -> "lichess.org",
    'url -> s"$netBaseUrl${routes.Round.watcher(pov.game.id, pov.color.name).url}",
    'description -> describePov(pov))

  def describePov(pov: Pov) = {
    import pov._
    val p1 = playerText(player, withRating = true)
    val p2 = playerText(opponent, withRating = true)
    val speedAndClock = game.clock.fold("unlimited") { c =>
      s"${chess.Speed(c.some).shortName} (${c.showCondensed})"
    }
    val mode = game.mode.name
    val variant = if (game.variant == chess.Variant.FromPosition) "position setup chess"
    else if (game.variant.exotic) game.variant.name else "chess"
    import chess.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate) => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime) => s"${playerText(l)} forfeits by time"
      case (_, _, Draw | Stalemate) => "Game is a draw"
      case (_, _, Aborted) => "Game has been aborted"
      case _ => "Game is still being played"
    }
    val moves = s"${game.turns} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: Variant)(implicit ctx: UserContext) = variant match {
    case Variant.Standard     => trans.standard.str()
    case Variant.Chess960     => "Chess960"
    case Variant.FromPosition => trans.fromPosition.str()
  }

  def variantNameNoCtx(variant: Variant) = variant match {
    case Variant.Standard     => trans.standard.en()
    case Variant.Chess960     => "Chess960"
    case Variant.FromPosition => trans.fromPosition.en()
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

  def playerUsername(player: Player, withRating: Boolean = true, withTitle: Boolean = true) =
    Namer.player(player, withRating, withTitle)(lightUser)

  def playerText(player: Player, withRating: Boolean = false) =
    player.aiLevel.fold(
      player.userId.flatMap(lightUser).fold("Anon.") { u =>
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
    case _       => Html("")
  }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c => " • " + c.show }
    s"$u1 vs $u2$clock"
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

  def gameFen(game: Game, color: Color, ownerLink: Boolean = false, tv: Boolean = false)(implicit ctx: UserContext) = Html {
    val owner = ownerLink.fold(ctx.me flatMap game.player, none)
    var isLive = game.isBeingPlayed
    val url = owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    }
    val href = tv.fold(routes.Tv.index, url)
    val title = gameTitle(game, color)
    val cssClass = isLive ?? ("live live_" + game.id)
    val live = isLive ?? game.id
    val fen = Forsyth exportBoard game.toChess.board
    val lastMove = ~game.castleLastMoveTime.lastMoveString
    s"""<a href="$href" title="$title" class="mini_board parse_fen $cssClass" data-live="$live" data-color="${color.name}" data-fen="$fen" data-lastmove="$lastMove"></a>"""
  }

  def gameFenNoCtx(game: Game, color: Color, tv: Boolean = false, blank: Boolean = false) = Html {
    var isLive = game.isBeingPlayed
    """<a href="%s%s" title="%s" class="mini_board parse_fen %s" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"%s></a>""".format(
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
