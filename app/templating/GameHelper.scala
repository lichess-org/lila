package lila.app
package templating

import chess.format.Forsyth
import chess.{ Status => S, Color, Clock, Mode }
import controllers.routes
import play.twirl.api.Html
import scalatags.Text.Frag

import lila.common.String.html.escapeHtml
import lila.game.{ Game, Player, Namer, Pov }
import lila.i18n.{ I18nKeys, enLang }
import lila.user.{ User, UserContext }

trait GameHelper { self: I18nHelper with UserHelper with AiHelper with StringHelper with HtmlHelper with ChessgroundHelper =>

  def netBaseUrl: String
  def cdnUrl(path: String): String

  def povOpenGraph(pov: Pov) = lila.app.ui.OpenGraph(
    image = cdnUrl(routes.Export.png(pov.gameId).url).some,
    title = titleGame(pov.game),
    url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
    description = describePov(pov)
  )

  def titleGame(g: Game) = {
    val speed = chess.Speed(g.clock.map(_.config)).name
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
        s"${chess.Speed(c.config).name} (${c.config.show})"
      }
    val mode = game.mode.name
    val variant = if (game.variant == chess.variant.FromPosition) "position setup chess"
    else if (game.variant.exotic) game.variant.name else "chess"
    import chess.Status._
    val result = (game.winner, game.loser, game.status) match {
      case (Some(w), _, Mate) => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime) => s"${playerText(l)} forfeits by time"
      case (Some(w), _, UnknownFinish) => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish) => "Game is a draw"
      case (_, _, Aborted) => "Game has been aborted"
      case (_, _, VariantEnd) => game.variant match {
        case chess.variant.KingOfTheHill => "King in the center"
        case chess.variant.ThreeCheck => "Three checks"
        case chess.variant.Antichess => "Lose all your pieces to win"
        case chess.variant.Atomic => "Explode or mate your opponent's king to win"
        case chess.variant.Horde => "Destroy the horde to win"
        case chess.variant.RacingKings => "Race to the eighth rank to win"
        case chess.variant.Crazyhouse => "Drop captured pieces on the board"
        case _ => "Variant ending"
      }
      case _ => "Game is still being played"
    }
    val moves = s"${game.chess.fullMoveNumber} moves"
    s"$p1 plays $p2 in a $mode $speedAndClock game of $variant. $result after $moves. Click to replay, analyse, and discuss the game!"
  }

  def variantName(variant: chess.variant.Variant)(implicit ctx: UserContext) = variant match {
    case chess.variant.Standard => I18nKeys.standard.txt()
    case chess.variant.FromPosition => I18nKeys.fromPosition.txt()
    case v => v.name
  }

  def variantNameNoCtx(variant: chess.variant.Variant) = variant match {
    case chess.variant.Standard => I18nKeys.standard.literalTxtTo(enLang)
    case chess.variant.FromPosition => I18nKeys.fromPosition.literalTxtTo(enLang)
    case v => v.name
  }

  def shortClockName(clock: Option[Clock.Config])(implicit ctx: UserContext): Html =
    clock.fold(I18nKeys.unlimited())(shortClockName)

  def shortClockName(clock: Clock.Config): Html = Html(clock.show)

  def modeName(mode: Mode)(implicit ctx: UserContext): String = mode match {
    case Mode.Casual => I18nKeys.casual.txt()
    case Mode.Rated => I18nKeys.rated.txt()
  }

  def modeNameNoCtx(mode: Mode): String = mode match {
    case Mode.Casual => I18nKeys.casual.literalTxtTo(enLang)
    case Mode.Rated => I18nKeys.rated.literalTxtTo(enLang)
  }

  def playerUsername(player: Player, withRating: Boolean = true, withTitle: Boolean = true) =
    Namer.player(player, withRating, withTitle)(lightUser)

  def playerText(player: Player, withRating: Boolean = false) =
    Namer.playerText(player, withRating)(lightUser)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    Namer.gameVsText(game, withRatings)(lightUser)

  val berserkIconSpan = """<span data-icon="`"></span>"""
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
    link: Boolean = true
  )(implicit ctx: UserContext) = Html {
    val statusIcon =
      if (withStatus) statusIconSpan
      else if (withBerserk && player.berserk) berserkIconSpan
      else ""
    player.userId.flatMap(lightUser) match {
      case None =>
        val klass = cssClass.??(" " + _)
        val content = (player.aiLevel, player.name) match {
          case (Some(level), _) => aiNameHtml(level, withRating).body
          case (_, Some(name)) => escapeHtml(name).body
          case _ => User.anonymous
        }
        s"""<span class="user_link$klass">$content$statusIcon</span>"""
      case Some(user) =>
        val klass = userClass(user.id, cssClass, withOnline)
        val href = s"${routes.User show user.name}${if (mod) "?mod" else ""}"
        val content = playerUsername(player, withRating)
        val diff = (player.ratingDiff ifTrue withDiff) ?? showRatingDiff
        val mark = engine ?? s"""<span class="engine_mark" title="${I18nKeys.thisPlayerUsesChessComputerAssistance()}"></span>"""
        val icon = withOnline ?? lineIcon(user)
        val space = if (withOnline) "&nbsp;" else ""
        val tag = if (link) "a" else "span"
        s"""<$tag $klass href="$href">$icon$space$content$diff$mark</$tag>$statusIcon"""
    }
  }

  def gameEndStatus(game: Game)(implicit ctx: UserContext): String = game.status match {
    case S.Aborted => I18nKeys.gameAborted.txt()
    case S.Mate => I18nKeys.checkmate.txt()
    case S.Resign => game.loser match {
      case Some(p) if p.color.white => I18nKeys.whiteResigned.txt()
      case _ => I18nKeys.blackResigned.txt()
    }
    case S.UnknownFinish => I18nKeys.finished.txt()
    case S.Stalemate => I18nKeys.stalemate.txt()
    case S.Timeout => game.loser match {
      case Some(p) if p.color.white => I18nKeys.whiteLeftTheGame.txt()
      case Some(_) => I18nKeys.blackLeftTheGame.txt()
      case None => I18nKeys.draw.txt()
    }
    case S.Draw => I18nKeys.draw.txt()
    case S.Outoftime => I18nKeys.timeOut.txt()
    case S.NoStart => {
      val color = game.loser.fold(Color.white)(_.color).name.capitalize
      s"$color didn't move"
    }
    case S.Cheat => "Cheat detected"
    case S.VariantEnd => game.variant match {
      case chess.variant.KingOfTheHill => I18nKeys.kingInTheCenter.txt()
      case chess.variant.ThreeCheck => I18nKeys.threeChecks.txt()
      case chess.variant.RacingKings => I18nKeys.raceFinished.txt()
      case _ => I18nKeys.variantEnding.txt()
    }
    case _ => ""
  }

  private def gameTitle(game: Game, color: Color): String = {
    val u1 = playerText(game player color, withRating = true)
    val u2 = playerText(game opponent color, withRating = true)
    val clock = game.clock ?? { c => " • " + c.config.show }
    val variant = game.variant.exotic ?? s" • ${game.variant.name}"
    s"$u1 vs $u2$clock$variant"
  }

  // whiteUsername 1-0 blackUsername
  def gameSummary(whiteUserId: String, blackUserId: String, finished: Boolean, result: Option[Boolean]) = {
    val res = if (finished) chess.Color.showResult(result map Color.apply) else "*"
    s"${usernameOrId(whiteUserId)} $res ${usernameOrId(blackUserId)}"
  }

  def gameResult(game: Game) =
    if (game.finished) chess.Color.showResult(game.winnerColor)
    else "*"

  def gameLink(
    game: Game,
    color: Color,
    ownerLink: Boolean = false,
    tv: Boolean = false
  )(implicit ctx: UserContext): String = {
    val owner = ownerLink ?? ctx.me.flatMap(game.player)
    if (tv) routes.Tv.index else owner.fold(routes.Round.watcher(game.id, color.name)) { o =>
      routes.Round.player(game fullIdOf o.color)
    }
  }.toString

  def gameFen(
    pov: Pov,
    ownerLink: Boolean = false,
    tv: Boolean = false,
    withTitle: Boolean = true,
    withLink: Boolean = true,
    withLive: Boolean = true
  )(implicit ctx: UserContext) = Html {
    val game = pov.game
    val isLive = withLive && game.isBeingPlayed
    val href = withLink ?? s"""href="${gameLink(game, pov.color, ownerLink, tv)}""""
    val title = withTitle ?? s"""title="${gameTitle(game, pov.color)}""""
    val cssClass = isLive ?? ("live live_" + game.id)
    val live = isLive ?? game.id
    val fen = Forsyth exportBoard game.board
    val lastMove = ~game.lastMoveKeys
    val variant = game.variant.key
    val tag = if (withLink) "a" else "span"
    s"""<$tag $href $title class="mini_board mini_board_${game.id} parse_fen is2d $cssClass $variant" data-live="$live" data-color="${pov.color.name}" data-fen="$fen" data-lastmove="$lastMove">$miniBoardContent</$tag>"""
  }

  def gameFenNoCtx(pov: Pov, tv: Boolean = false, blank: Boolean = false) = Html {
    val isLive = pov.game.isBeingPlayed
    val variant = pov.game.variant.key
    s"""<a href="%s%s" title="%s" class="mini_board mini_board_${pov.gameId} parse_fen is2d %s $variant" data-live="%s" data-color="%s" data-fen="%s" data-lastmove="%s"%s>$miniBoardContent</a>""".format(
      blank ?? netBaseUrl,
      if (tv) routes.Tv.index else routes.Round.watcher(pov.gameId, pov.color.name),
      gameTitle(pov.game, pov.color),
      isLive ?? ("live live_" + pov.gameId),
      isLive ?? pov.gameId,
      pov.color.name,
      Forsyth exportBoard pov.game.board,
      ~pov.game.lastMoveKeys,
      blank ?? """ target="_blank""""
    )
  }

  def challengeTitle(c: lila.challenge.Challenge)(implicit ctx: UserContext) = {
    val speed = c.clock.map(_.config).fold(chess.Speed.Correspondence.name) { clock =>
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
      description = "Join the challenge or watch the game here."
    )
}
