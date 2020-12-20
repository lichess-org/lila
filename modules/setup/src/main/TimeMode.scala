package lila.setup

sealed abstract class TimeMode(val id: Int)

object TimeMode {

  case object Unlimited      extends TimeMode(0)
  case object RealTime       extends TimeMode(1)
  case object Correspondence extends TimeMode(2)

  val default = RealTime

  val all = List(Unlimited, RealTime, Correspondence)

  val ids = all map (_.id)

  val byId = all map { v =>
    (v.id, v)
  } toMap

  def apply(id: Int): Option[TimeMode] = byId get id

  def orDefault(id: Int) = apply(id) | default

  def ofGame(game: lila.game.Game) =
    if (game.hasClock) RealTime
    else if (game.hasCorrespondenceClock) Correspondence
    else Unlimited
}
