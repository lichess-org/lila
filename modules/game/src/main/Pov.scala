package lila.game

import scalalib.model.Seconds
import lila.core.game.*

object Pov:

  def list(game: Game): List[Pov] = game.players.mapList(lila.core.game.Pov(game, _))

  private inline def orInf(inline sec: Option[Seconds]) = sec.getOrElse(Seconds(Int.MaxValue))
  private def isFresher(a: Pov, b: Pov) = a.game.movedAt.isAfter(b.game.movedAt)

  def priority(a: Pov, b: Pov) =
    if !a.isMyTurn && !b.isMyTurn then isFresher(a, b)
    else if !a.isMyTurn && b.isMyTurn then false
    else if a.isMyTurn && !b.isMyTurn then true
    // first move has priority over games with more than 30s left
    else if orInf(a.remainingSeconds) < 30 && orInf(b.remainingSeconds) > 30 then true
    else if orInf(b.remainingSeconds) < 30 && orInf(a.remainingSeconds) > 30 then false
    else if !a.hasMoved && b.hasMoved then true
    else if !b.hasMoved && a.hasMoved then false
    else orInf(a.remainingSeconds) < orInf(b.remainingSeconds)
