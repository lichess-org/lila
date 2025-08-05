package lila.setup

enum TimeMode(val id: Int):

  case Unlimited extends TimeMode(0)
  case RealTime extends TimeMode(1)
  case Correspondence extends TimeMode(2)

object TimeMode:
  val default = RealTime

  val ids = values.map(_.id)
  val byId = values.mapBy(_.id)

  def apply(id: Int): Option[TimeMode] = byId.get(id)

  def orDefault(id: Int) = apply(id) | default

  def ofGame(game: Game) =
    if game.hasClock then RealTime
    else if game.hasCorrespondenceClock then Correspondence
    else Unlimited
