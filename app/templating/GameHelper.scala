package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status => S, Color, Clock, Mode }
import controllers.routes
import play.api.mvc.Call
import play.twirl.api.Html

import lila.game.{ Game, Player, Namer, Pov }
import lila.user.{ User, UserContext }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper =>

  def netBaseUrl: String
  def staticUrl(path: String): String
  def cdnUrl(path: String): String

  def povOpenGraph(pov: Pov) = lila.app.ui.OpenGraph(
    image = cdnUrl(routes.Export.png(pov.game.id).url).some,
    title = titleGame(pov.game),
    url = s"$netBaseUrl${routes.Round.watcher(pov.game.id, pov.color.name).url}",
    description = describePov(pov))

  def titleGame(g: Game) = {
    val speed = chess.Speed(g.clock).name
    val variant = g.variant.exotic ?? s" ${g.variant.name}"
    s"$speed$variant Chess • ${playerText(g.whitePlayer)} vs ${playerText(g.blackPlayer)}"
  }

  def describePov(pov: Pov) = {
    import pov._
    val p1 = playerText(player, withRating = true)
    val p2 = playerText(opponent, withRating = true)
    val speedAndClock =
      if (game.imported) "imported"
      else game.clock.fold(chess.Speed.Correspondence.name) { c =>
        s"${chess.Speed(c.some).name} (${c.show})"
      }
    val mode = game.mode.name
    val variant = if (game.variant == chess.variant.FromPosition) "position setup chess"
    else if (game.variant.exotic) game.variant.name else "chess"
    import chess.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} forfeits by time"
      case (Some(w), _, UnknownFinish)                      => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish)         => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case (_, _, VariantEnd) => game.variant match {
        case chess.variant.KingOfTheHill => "King in the center"
        case chess.variant.ThreeCheck    => "Three checks"
        case chess.variant.Antichess     => "Lose all your pieces to win"
        case chess.variant.Atomic        => "Explode or mate your opponent's king to win"
        case chess.variant.Horde         => "Destroy the horde to win"
        case chess.variant.RacingKings   => "Race to the eighth rank to win"
        case chess.variant.Crazyhouse    => "Drop captured pieces on the board"
        case _                           => "Variant ending"
      }
      case _ => "Game is still being played"
    }
    val moves = s"${game.toChess.fullMoveNumber} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: chess.variant.Variant)(implicit ctx: UserContext) = variant match {
    case chess.variant.Standard     => trans.standard.str()
    case chess.variant.FromPosition => trans.fromPosition.str()
    case v                          => v.name
  }

  def variantNameNoCtx(variant: chess.variant.Variant) = variant match {
    case chess.variant.Standard     => trans.standard.en()
    case chess.variant.FromPosition => trans.fromPosition.en()
    case v                          => v.name
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
    Namer.playerText(player, withRating)(lightUser)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    Namer.gameVsText(game, withRatings)(lightUser)

  val berserkIconSpan = """<span data-icon="`"></span>"""
  val berserkIconSpanHtml = Html(berserkIconSpan)
  val statusIconSpan = """<span class="status"></span>"""

  def playerLink(
    player: Player,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withRating: Boolean = true,
    withDiff: Boolean = true,
    engine: Boolean = false,
    withStatus: Boolean = false,
    withBerserk: Boolean = false,
    mod: Boolean = false,
    link: Boolean = true)(implicit ctx: UserContext) = Html {
    val statusIcon =
      if (withStatus) statusIconSpan
      else if (withBerserk && player.berserk) berserkIconSpan
      else ""
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        val content = (player.aiLevel, player.name) match {
          case (Some(level), _) => aiNameHtml(level, withRating).body
          case (_, Some(name))  => escape(name)
          case _                => User.anonymous
        }
        s"""<span class="user_link$klass">$content$statusIcon</span>"""
      case Some(user) =>
        val klass = userClass(user.id, cssClass, withOnline)
        val href = s"${routes.User show user.name}${if (mod) "?mod" else ""}"
        val content = playerUsername(player, withRating)
        val diff = (player.ratingDiff ifTrue withDiff).fold(Html(""))(showRatingDiff)
        val mark = engine ?? s"""<span class="engine_mark" title="${trans.thisPlayerUsesChessComputerAssistance()}"></span>"""
        val dataIcon = withOnline ?? """data-icon="r""""
        val space = if (withOnline) "&nbsp;" else ""
        val tag = if (link) "a" else "span"
        s"""<$tag $dataIcon $klass href="$href">$space$content$diff$mark</$tag>$statusIcon"""
    }
  }

  def gameEndStatus(game: Game)(implicit ctx: UserContext): Html = game.status match {
    case S.Aborted => trans.gameAborted()
    case S.Mate    => trans.checkmate()
    case S.Resign => game.loser match {
      case Some(p) if p.color.white => trans.whiteResigned()
      case _                        => trans.blackResigned()
    }
    case S.UnknownFinish => trans.finished()
    case S.Stalemate     => trans.stalemate()
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
      case chess.variant.KingOfTheHill => trans.kingInTheCenter()
      case chess.variant.ThreeCheck    => trans.threeChecks()
      case chess.variant.RacingKings   => trans.raceFinished()
      case _                           => trans.variantEnding()
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

  def gameResult(game: Game) =
    if (game.finished) game.winnerColor match {
      case Some(chess.White) => "1-0"
      case Some(chess.Black) => "0-1"
      case None              => "½-½"
    }
    else "*"

  lazy val miniBoardContent = Html("""<div class="cg-board-wrap"><div class="cg-board"></div></div>""")

  def gameLink(
    game: Game,
    color: Color,
    ownerLink: Boolean = false,
    tv: Boolean = false)(implicit ctx: UserContext): String = {
    val owner = ownerLink.fold(ctx.me flatMap game.player, none)
    val url = tv.fold(routes.Tv.index, owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    })
    url.toString
  }

  def gameFen(
    pov: Pov,
    ownerLink: Boolean = false,
    tv: Boolean = false,
    withTitle: Boolean = true,
    withLink: Boolean = true,
    withLive: Boolean = true)(implicit ctx: UserContext) = Html {
    val game = pov.game
    var isLive = withLive && game.isBeingPlayed
    val href = withLink ?? s"""href="${gameLink(game, pov.color, ownerLink, tv)}""""
    val title = withTitle ?? s"""title="${gameTitle(game, pov.color)}""""
    val cssClass = isLive ?? ("live live_" + game.id)
    val live = isLive ?? game.id
    val fen = Forsyth exportBoard game.toChess.board
    val lastMove = ~game.castleLastMoveTime.lastMoveString
    val variant = game.variant.key
    val tag = if (withLink) "a" else "span"
    s"""<$tag $href $title class="mini_board mini_board_${game.id} parse_fen is2d $cssClass $variant" data-live="$live" data-color="${pov.color.name}" data-fen="$fen" data-lastmove="$lastMove">$miniBoardContent</$tag>"""
  }

  def gameFenNoCtx(pov: Pov, tv: Boolean = false, blank: Boolean = false) = Html {
    var isLive = pov.game.isBeingPlayed
    val variant = pov.game.variant.key
    s"""<a href="%s%s" title="%s" class="mini_board mini_board_${pov.game.id} parse_fen is2d %s $variant" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"%s>$miniBoardContent</a>""".format(
      blank ?? netBaseUrl,
      tv.fold(routes.Tv.index, routes.Round.watcher(pov.game.id, pov.color.name)),
      gameTitle(pov.game, pov.color),
      isLive ?? ("live live_" + pov.game.id),
      isLive ?? pov.game.id,
      pov.color.name,
      Forsyth exportBoard pov.game.toChess.board,
      ~pov.game.castleLastMoveTime.lastMoveString,
      blank ?? """ target="_blank"""")
  }

  def challengeTitle(c: lila.challenge.Challenge)(implicit ctx: UserContext) = {
    val speed = c.clock.map(_.chessClock).fold(trans.unlimited.str()) { clock =>
      s"${chess.Speed(clock).name} (${clock.show})"
    }
    val variant = c.variant.exotic ?? s" ${c.variant.name}"
    val challenger = c.challenger.fold(
      _ => User.anonymous,
      reg => s"${usernameOrId(reg.id)} (${reg.rating.show})"
    )
    val players = c.destUser.fold(s"Challenge from $challenger") { dest =>
      s"$challenger challenges ${usernameOrId(dest.id)} (${dest.rating.show})"
    }
    s"$speed$variant ${c.mode.name} Chess • $players"
  }

  def challengeOpenGraph(c: lila.challenge.Challenge)(implicit ctx: UserContext) =
    lila.app.ui.OpenGraph(
      title = challengeTitle(c),
      url = s"$netBaseUrl${routes.Round.watcher(c.id, chess.White.name).url}",
      description = "Join the challenge or watch the game here.")
}
