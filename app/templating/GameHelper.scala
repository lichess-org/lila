package lila.app
package templating

import chess.{ Black, Clock, Color, Mode, Outcome, Ply, Status as S, White }
import controllers.routes
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.core.LightUser
import lila.game.{ Game, LightPlayer, Namer, Player, Pov }
import lila.core.i18n.{ I18nKey as trans, defaultLang, Translate }

trait GameHelper:
  self: RouterHelper & I18nHelper & UserHelper & AiHelper & StringHelper & NumberHelper & ChessgroundHelper =>

  def netBaseUrl: String
  def cdnUrl(path: String): String

  def povOpenGraph(pov: Pov) =
    lila.app.ui.OpenGraph(
      image = cdnUrl(routes.Export.gameThumbnail(pov.gameId, None, None).url).some,
      title = titleGame(pov.game),
      url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
      description = describePov(pov)
    )

  def titleGame(g: Game) =
    val speed   = chess.Speed(g.clock.map(_.config)).name
    val variant = g.variant.exotic.so(s" ${g.variant.name}")
    s"$speed$variant Chess • ${playerText(g.whitePlayer)} vs ${playerText(g.blackPlayer)}"

  def describePov(pov: Pov) =
    import pov.*
    val p1    = playerText(game.whitePlayer, withRating = true)
    val p2    = playerText(game.blackPlayer, withRating = true)
    val plays = if game.finishedOrAborted then "played" else "is playing"
    val speedAndClock =
      if game.imported then "imported"
      else
        game.clock.fold(chess.Speed.Correspondence.name): c =>
          s"${chess.Speed(c.config).name} (${c.config.show})"

    val mode = game.mode.name
    val variant =
      if game.variant == chess.variant.FromPosition
      then "position setup chess"
      else if game.variant.exotic
      then game.variant.name
      else "chess"
    import chess.Status.*
    val result = (game.winner, game.loser, game.status) match
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} ran out of time"
      case (Some(w), _, UnknownFinish | VariantEnd)         => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish)         => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case _ if game.finished                               => "Game ended"
      case _                                                => "Game is still ongoing"
    val moves = (game.ply.value - game.startedAtPly.value + 1) / 2
    s"$p1 $plays $p2 in a $mode $speedAndClock game of $variant. $result after ${pluralize("move", moves)}. Click to replay, analyse, and discuss the game!"

  def shortClockName(clock: Option[Clock.Config])(using t: Translate): Frag =
    clock.fold[Frag](trans.site.unlimited())(shortClockName)

  def shortClockName(clock: Clock.Config): Frag = raw(clock.show)

  def shortClockName(game: Game)(using Translate): Frag =
    game.correspondenceClock
      .map(c => trans.site.nbDays(c.daysPerTurn))
      .orElse(game.clock.map(_.config).map(shortClockName))
      .getOrElse(trans.site.unlimited())

  def modeName(mode: Mode)(using Translate): String =
    mode match
      case Mode.Casual => trans.site.casual.txt()
      case Mode.Rated  => trans.site.rated.txt()

  def playerUsername(
      player: LightPlayer,
      user: Option[LightUser],
      withRating: Boolean = true,
      withTitle: Boolean = true
  )(using Translate): Frag =
    player.aiLevel.fold[Frag](
      user
        .fold[Frag](trans.site.anonymous.txt()): user =>
          frag(
            titleTag(withTitle.so(user.title)),
            user.name,
            user.flair.map(userFlair),
            withRating.option(
              frag(
                " (",
                player.rating.fold(frag("?")): rating =>
                  if player.provisional.yes then
                    abbr(title := trans.perfStat.notEnoughRatedGames.txt())(rating, "?")
                  else rating,
                ")"
              )
            )
          )
    ): level =>
      frag(aiName(level))

  def playerText(player: Player, withRating: Boolean = false): String =
    Namer.playerTextBlocking(player, withRating)(using lightUser)

  def gameVsText(game: Game, withRatings: Boolean = false): String =
    Namer.gameVsTextBlocking(game, withRatings)(using lightUser)

  val berserkIconSpan = iconTag(lila.common.licon.Berserk)

  def playerLink(
      player: Player,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withRating: Boolean = true,
      withDiff: Boolean = true,
      engine: Boolean = false,
      withBerserk: Boolean = false,
      mod: Boolean = false,
      link: Boolean = true
  )(using ctx: Context): Frag =
    val statusIcon = (withBerserk && player.berserk).option(berserkIconSpan)
    player.userId.flatMap(lightUser) match
      case None =>
        val klass = cssClass.so(" " + _)
        span(cls := s"user-link$klass")(
          (player.aiLevel, player.name) match
            case (Some(level), _) => aiNameFrag(level)
            case (_, Some(name))  => name
            case _                => trans.site.anonymous()
          ,
          player.rating.ifTrue(withRating && ctx.pref.showRatings).map { rating => s" ($rating)" },
          statusIcon
        )
      case Some(user) =>
        frag(
          (if link then a else span) (
            cls := userClass(user.id, cssClass, withOnline),
            (if link then href else dataHref) := s"${routes.User.show(user.name)}${if mod then "?mod" else ""}"
          )(
            withOnline.option(frag(lineIcon(user), " ")),
            playerUsername(
              player.light,
              user.some,
              withRating = withRating && ctx.pref.showRatings
            ),
            (player.ratingDiff.ifTrue(withDiff && ctx.pref.showRatings)).map { d =>
              frag(" ", showRatingDiff(d))
            },
            tosMark(engine)
          ),
          statusIcon
        )

  def lightPlayerLink(
      player: LightPlayer,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withRating: Boolean = true,
      withDiff: Boolean = true,
      engine: Boolean = false,
      withBerserk: Boolean = false,
      mod: Boolean = false,
      link: Boolean = true
  )(using ctx: Context): Frag =
    val statusIcon = (withBerserk && player.berserk).option(berserkIconSpan)
    player.userId.flatMap(lightUser) match
      case None =>
        val klass = cssClass.so(" " + _)
        span(cls := s"user-link$klass")(
          player.aiLevel.fold(trans.site.anonymous())(aiNameFrag),
          player.rating.ifTrue(withRating && ctx.pref.showRatings).map { rating => s" ($rating)" },
          statusIcon
        )
      case Some(user) =>
        frag(
          (if link then a else span) (
            cls := userClass(user.id, cssClass, withOnline),
            (if link then href else dataHref) := s"${routes.User.show(user.name)}${if mod then "?mod" else ""}"
          )(
            withOnline.option(frag(lineIcon(user), " ")),
            playerUsername(
              player,
              user.some,
              withRating = withRating && ctx.pref.showRatings
            ),
            (player.ratingDiff.ifTrue(withDiff && ctx.pref.showRatings)).map { d =>
              frag(" ", showRatingDiff(d))
            },
            tosMark(engine)
          ),
          statusIcon
        )

  private def tosMark(mark: Boolean)(using Translate): Option[Tag] =
    mark.option(span(cls := "tos_violation", title := trans.site.thisAccountViolatedTos.txt()))

  def gameEndStatus(game: Game)(using Translate): String =
    game.status match
      case S.Aborted => trans.site.gameAborted.txt()
      case S.Mate    => trans.site.checkmate.txt()
      case S.Resign =>
        (if game.loser.exists(_.color.white) then trans.site.whiteResigned else trans.site.blackResigned)
          .txt()
      case S.UnknownFinish => trans.site.finished.txt()
      case S.Stalemate     => trans.site.stalemate.txt()
      case S.Timeout =>
        (game.loser, game.turnColor) match
          case (Some(p), _) if p.color.white => trans.site.whiteLeftTheGame.txt()
          case (Some(_), _)                  => trans.site.blackLeftTheGame.txt()
          case (None, White) => trans.site.whiteLeftTheGame.txt() + " • " + trans.site.draw.txt()
          case (None, Black) => trans.site.blackLeftTheGame.txt() + " • " + trans.site.draw.txt()
      case S.Draw =>
        import lila.game.DrawReason.*
        game.drawReason match
          case Some(MutualAgreement) => trans.site.drawByMutualAgreement.txt()
          case Some(FiftyMoves) => trans.site.fiftyMovesWithoutProgress.txt() + " • " + trans.site.draw.txt()
          case Some(ThreefoldRepetition) =>
            trans.site.threefoldRepetition.txt() + " • " + trans.site.draw.txt()
          case Some(InsufficientMaterial) =>
            trans.site.insufficientMaterial.txt() + " • " + trans.site.draw.txt()
          case _ => trans.site.draw.txt()
      case S.Outoftime =>
        (game.turnColor, game.loser) match
          case (White, Some(_)) => trans.site.whiteTimeOut.txt()
          case (White, None)    => trans.site.whiteTimeOut.txt() + " • " + trans.site.draw.txt()
          case (Black, Some(_)) => trans.site.blackTimeOut.txt()
          case (Black, None)    => trans.site.blackTimeOut.txt() + " • " + trans.site.draw.txt()
      case S.NoStart =>
        (if game.loser.exists(_.color.white) then trans.site.whiteDidntMove else trans.site.blackDidntMove)
          .txt()
      case S.Cheat => trans.site.cheatDetected.txt()
      case S.VariantEnd =>
        game.variant match
          case chess.variant.KingOfTheHill => trans.site.kingInTheCenter.txt()
          case chess.variant.ThreeCheck    => trans.site.threeChecks.txt()
          case chess.variant.RacingKings   => trans.site.raceFinished.txt()
          case _                           => trans.site.variantEnding.txt()
      case _ => ""

  def gameTitle(game: Game, color: Color): String =
    val u1 = playerText(game.player(color), withRating = true)
    val u2 = playerText(game.opponent(color), withRating = true)
    val clock = game.clock.so: c =>
      " • " + c.config.show
    val variant = game.variant.exotic.so(s" • ${game.variant.name}")
    s"$u1 vs $u2$clock$variant"

  def gameResult(game: Game) =
    Outcome.showResult(game.finished.option(Outcome(game.winnerColor)))

  def gameLink(
      game: Game,
      color: Color,
      ownerLink: Boolean = false,
      tv: Boolean = false
  )(using ctx: Context): String = {
    val owner = ownerLink.so(ctx.me.flatMap(game.player))
    if tv then routes.Tv.index
    else
      owner.fold(routes.Round.watcher(game.id, color.name)): o =>
        routes.Round.player(game.fullIdOf(o.color))
  }.toString

  def gameLink(pov: Pov)(using Context): String = gameLink(pov.game, pov.color)

  def challengeTitle(c: lila.challenge.Challenge)(using ctx: Context) =
    val speed = c.clock.map(_.config).fold(chess.Speed.Correspondence.name) { clock =>
      s"${chess.Speed(clock).name} (${clock.show})"
    }
    val variant = c.variant.exotic.so(s" ${c.variant.name}")
    val challenger = c.challengerUser.fold(trans.site.anonymous.txt()): reg =>
      s"${titleNameOrId(reg.id)}${ctx.pref.showRatings.so(s" (${reg.rating.show})")}"
    val players =
      if c.isOpen then "Open challenge"
      else
        c.destUser.fold(s"Challenge from $challenger"): dest =>
          s"$challenger challenges ${titleNameOrId(dest.id)}${ctx.pref.showRatings.so(s" (${dest.rating.show})")}"
    s"$speed$variant ${c.mode.name} Chess • $players"

  def challengeOpenGraph(c: lila.challenge.Challenge)(using Context) =
    lila.app.ui.OpenGraph(
      title = challengeTitle(c),
      url = s"$netBaseUrl${routes.Round.watcher(c.id, chess.White.name).url}",
      description = "Join the challenge or watch the game here."
    )
