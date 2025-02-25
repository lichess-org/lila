package lila.round

import chess.{ ByColor, Color }

import scalalib.data.Preload
import lila.game.GameExt.withClock
import lila.game.{ Event, Progress }
import lila.pref.{ Pref, PrefApi }

final class Moretimer(
    messenger: Messenger,
    prefApi: PrefApi
)(using Executor):

  private val minTime = 5.seconds
  private val maxTime = 60.seconds

  // pov of the player giving more time
  def apply(pov: Pov, duration: FiniteDuration, force: Boolean): Fu[Option[Progress]] =
    isAllowedIn(pov.game, Preload.none, force).map:
      case false =>
        logger.warn(s"[moretimer] not allowed on ${pov.game.id}")
        none
      case true =>
        if pov.game.clock.exists(_.moretimeable(!pov.color))
        then give(pov.game, List(!pov.color), duration).some
        else if pov.game.correspondenceClock.exists(_.moretimeable(!pov.color))
        then
          messenger.volatile(pov.game, s"${!pov.color} gets more time")
          val p = Progress(pov.game, pov.game.copy(movedAt = nowInstant))
          p.game.correspondenceClock.map(Event.CorrespondenceClock.apply).foldLeft(p)(_ + _).some
        else none

  def isAllowedIn(game: Game, prefs: Preload[ByColor[Pref]], force: Boolean): Fu[Boolean] =
    (game.playable && !game.isUnlimited && game.canTakebackOrAddTime).so:
      if force then fuccess(true)
      else (!game.hasRule(_.noGiveTime)).so(isAllowedByPrefs(game, prefs))

  private[round] def give(game: Game, colors: List[Color], unchecked: FiniteDuration): Progress =
    game.clock.fold(Progress(game)): clock =>
      val duration =
        if unchecked < minTime then minTime
        else if unchecked > maxTime then maxTime
        else unchecked
      val centis = duration.toCentis
      val newClock = colors.foldLeft(clock): (c, color) =>
        c.giveTime(color, centis)
      colors.foreach: c =>
        messenger.volatile(game, s"$c + ${duration.toSeconds} seconds")
      (game.withClock(newClock)) ++ colors.map { Event.ClockInc(_, centis, newClock) }

  private def isAllowedByPrefs(game: Game, prefs: Preload[ByColor[Pref]]): Fu[Boolean] =
    prefs
      .orLoad:
        prefApi.byId(game.userIdPair)
      .dmap:
        _.forall: p =>
          p.moretime == Pref.Moretime.ALWAYS || (p.moretime == Pref.Moretime.CASUAL && game.casual)
