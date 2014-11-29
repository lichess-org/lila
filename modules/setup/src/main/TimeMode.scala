package lila.setup

sealed abstract class TimeMode(val id: Int)

object TimeMode {

  case object Unlimited extends TimeMode(0)
  case object Clock extends TimeMode(1)
  case object Correspondance extends TimeMode(2)

  val all = List(Unlimited, Clock, Correspondance)

  val ids = all map (_.id)

  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[TimeMode] = byId get id
}

