package lila.game

import cats.derived.*

enum Source(val id: Int) derives Eq:

  def name = toString.toLowerCase

  case Lobby      extends Source(id = 1)
  case Friend     extends Source(id = 2)
  case Ai         extends Source(id = 3)
  case Api        extends Source(id = 4)
  case Arena      extends Source(id = 5)
  case Position   extends Source(id = 6)
  case Import     extends Source(id = 7)
  case ImportLive extends Source(id = 9)
  case Simul      extends Source(id = 10)
  case Relay      extends Source(id = 11)
  case Pool       extends Source(id = 12)
  case Swiss      extends Source(id = 13)

object Source:

  val byId = values.mapBy(_.id)

  val searchable = List(Lobby, Friend, Ai, Position, Import, Arena, Simul, Pool, Swiss)
  val expirable  = Set(Lobby, Arena, Pool, Swiss)

  def apply(id: Int): Option[Source] = byId get id
