package lila.game

sealed abstract class Source(val id: Int) {

  lazy val name = toString.toLowerCase
}

object Source {

  case object Lobby extends Source(id = 1)
  case object Friend extends Source(id = 2)
  case object Ai extends Source(id = 3)
  case object Api extends Source(id = 4)
  case object Tournament extends Source(id = 5)
  case object Position extends Source(id = 6)
  case object Import extends Source(id = 7)
  case object Pool extends Source(id = 8)
  case object ImportLive extends Source(id = 9)

  val all = List(Lobby, Friend, Ai, Api, Tournament, Position, Import, Pool)
  val byId = all map { v => (v.id, v) } toMap

  def apply(id: Int): Option[Source] = byId get id
}
