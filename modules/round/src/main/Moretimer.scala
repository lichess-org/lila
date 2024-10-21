package lila.round

import chess.{ ByColor, Color }

import lila.core.data.Preload
import lila.core.round.ClientError
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
    IfAllowed(pov.game, Preload.none, force):
      (pov.game
        .moretimeable(!pov.color))
        .so:
          if pov.game.hasClock
          then give(pov.game, List(!pov.color), duration).some
          else
            pov.game.hasCorrespondenceClock.option:
              messenger.volatile(pov.game, s"${!pov.color} gets more time")
              val p = correspondenceGiveTime(pov.game)
              p.game.correspondenceClock.map(Event.CorrespondenceClock.apply).fold(p)(p + _)

  TODO.duplicated(code)
  def isAllowedIn(game: Game, prefs: Preload[ByColor[Pref]], force: Boolean): Fu[Boolean] =
    (game.canTakebackOrAddTime && game.playable).so:
      if force then fuccess(true)
      else (!game.metadata.hasRule(_.noGiveTime)).so(isAllowedByPrefs(game, prefs))

  private def correspondenceGiveTime(g: Game) = Progress(g, g.copy(movedAt = nowInstant))

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

  private def IfAllowed[A](game: Game, prefs: Preload[ByColor[Pref]], force: Boolean)(f: => A): Fu[A] =
    if !game.playable then fufail(ClientError("[moretimer] game is over"))
    else if !game.canTakebackOrAddTime then fufail(ClientError("[moretimer] Can't add time to this game"))
    else if !force && game.metadata.hasRule(_.noGiveTime) then
      fufail(ClientError("[moretimer] game rules disallows it"))
    else
      isAllowedByPrefs(game, prefs).flatMap:
        if _ then fuccess(f)
        else fufail(ClientError("[moretimer] disallowed by preferences " + game.id))
