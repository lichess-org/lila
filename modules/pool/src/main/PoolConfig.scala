package lila.pool

import chess.Clock
import lila.rating.PerfType

case class PoolConfig(clock: chess.Clock) {

  val perfType = PerfType(chess.Speed(clock).key) | PerfType.Classical

  val id = PoolConfig.Id(clock.show)
}

object PoolConfig {

  case class Id(value: String) extends AnyVal

  // 10+5
  def parse(str: String): Option[PoolConfig] = str.split('+') match {
    case Array(lim, inc) => for {
      limit <- parseIntOption(lim)
      increment <- parseIntOption(inc)
    } yield PoolConfig(chess.Clock(limit * 60, increment))
    case _ => none
  }
}
