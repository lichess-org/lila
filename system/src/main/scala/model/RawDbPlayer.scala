package lila.system
package model

import lila.chess._

case class RawDbPlayer(
    id: String,
    color: String,
    ps: String,
    aiLevel: Option[Int],
    isWinner: Option[Boolean],
    evts: String = "",
    elo: Option[Int]) {

  def decode: Option[DbPlayer] = for {
    trueColor ← Color(color)
  } yield DbPlayer(
    id = id,
    color = trueColor,
    ps = ps,
    aiLevel = aiLevel,
    isWinner = isWinner,
    evts = evts,
    elo = elo
  )
}

object RawDbPlayer {

  def encode(dbPlayer: DbPlayer): RawDbPlayer = {
    import dbPlayer._
    RawDbPlayer(
      id = id,
      color = color.name,
      ps = ps,
      aiLevel = aiLevel,
      isWinner = isWinner,
      evts = evts,
      elo = elo
    )
  }
}
