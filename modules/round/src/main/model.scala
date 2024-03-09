package lila.round

import scala.concurrent.duration.FiniteDuration

import lila.game.Game
import lila.user.User

private case class MoretimeDuration(value: FiniteDuration) extends AnyVal

final class OnStart(f: Game.ID => Unit) extends (Game.ID => Unit) {
  def apply(g: Game.ID) = f(g)
}

final class TellRound(f: (Game.ID, Any) => Unit) extends ((Game.ID, Any) => Unit) {
  def apply(g: Game.ID, msg: Any) = f(g, msg)
}

final class IsSimulHost(f: User.ID => Fu[Boolean]) extends (User.ID => Fu[Boolean]) {
  def apply(u: User.ID) = f(u)
}

final private class ScheduleExpiration(f: Game => Unit) extends (Game => Unit) {
  def apply(g: Game) = f(g)
}

final class IsOfferingRematch(f: (Game.ID, shogi.Color) => Boolean)
    extends ((Game.ID, shogi.Color) => Boolean) {
  def apply(g: Game.ID, c: shogi.Color) = f(g, c)
}

final class IsOfferingResume(f: (Game.ID, shogi.Color) => Boolean)
    extends ((Game.ID, shogi.Color) => Boolean) {
  def apply(g: Game.ID, c: shogi.Color) = f(g, c)
}
