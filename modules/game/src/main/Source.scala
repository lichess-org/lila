package lila.game

sealed abstract class Source(val id: Int) {

  lazy val name = toString.toLowerCase
}

object Source {

  case object Lobby      extends Source(id = 1)
  case object Friend     extends Source(id = 2)
  case object Ai         extends Source(id = 3)
  case object Api        extends Source(id = 4)
  case object Tournament extends Source(id = 5)
  case object Position   extends Source(id = 6)
  case object Import     extends Source(id = 7)
  case object ImportLive extends Source(id = 9)
  case object Simul      extends Source(id = 10)
  case object Relay      extends Source(id = 11)
  case object Pool       extends Source(id = 12)
  case object Swiss      extends Source(id = 13)

  val all = List(Lobby, Friend, Ai, Api, Tournament, Position, Import, Simul, Relay, Pool, Swiss)
  val byId = all map { v =>
    (v.id, v)
  } toMap

  val searchable             = List(Lobby, Friend, Ai, Position, Import, Tournament, Simul, Pool, Swiss)
  val expirable: Set[Source] = Set(Lobby, Tournament, Pool, Swiss)

  def apply(id: Int): Option[Source] = byId get id
}
