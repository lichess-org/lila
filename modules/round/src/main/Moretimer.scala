package lila.round

import chess.Color

import lila.game.{ Event, Game, Pov, Progress }
import lila.pref.{ Pref, PrefApi }

final class Moretimer(
    messenger: Messenger,
    prefApi: PrefApi
)(using Executor):

  private val minTime = 5.seconds
  private val maxTime = 60.seconds

  // pov of the player giving more time
  def apply(pov: Pov, duration: FiniteDuration): Fu[Option[Progress]] =
    IfAllowed(pov.game):
      (pov.game moretimeable !pov.color).so:
        if pov.game.hasClock
        then give(pov.game, List(!pov.color), duration).some
        else
          pov.game.hasCorrespondenceClock.option:
            messenger.volatile(pov.game, s"${!pov.color} gets more time")
            val p = pov.game.correspondenceGiveTime
            p.game.correspondenceClock.map(Event.CorrespondenceClock.apply).fold(p)(p + _)

  def isAllowedIn(game: Game): Fu[Boolean] =
    (game.canTakebackOrAddTime && game.playable && !game.metadata.hasRule(_.NoGiveTime)) so
      isAllowedByPrefs(game)

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
      (game withClock newClock) ++ colors.map { Event.ClockInc(_, centis, newClock) }

  private def isAllowedByPrefs(game: Game): Fu[Boolean] =
    game.userIds
      .traverse:
        prefApi.get(_, _.moretime)
      .dmap:
        _.forall: p =>
          p == Pref.Moretime.ALWAYS || (p == Pref.Moretime.CASUAL && game.casual)

  private def IfAllowed[A](game: Game)(f: => A): Fu[A] =
    if !game.playable then fufail(ClientError("[moretimer] game is over " + game.id))
    else if !game.canTakebackOrAddTime || game.metadata.hasRule(_.NoGiveTime) then
      fufail(ClientError("[moretimer] game disallows it " + game.id))
    else
      isAllowedByPrefs(game) flatMap {
        if _ then fuccess(f)
        else fufail(ClientError("[moretimer] disallowed by preferences " + game.id))
      }
