package lila.system
package model

import lila.chess._

import com.mongodb.DBRef

case class RawDbPlayer(
    id: String,
    c: String,
    ps: String,
    aiLevel: Option[Int],
    w: Option[Boolean],
    evts: String = "",
    elo: Option[Int],
    isOfferingDraw: Option[Boolean],
    lastDrawOffer: Option[Int],
    user: Option[DBRef]) {

  def decode: Option[DbPlayer] = for {
    trueColor ← Color(c)
  } yield DbPlayer(
    id = id,
    color = trueColor,
    ps = ps,
    aiLevel = aiLevel,
    isWinner = w,
    evts = evts,
    elo = elo,
    isOfferingDraw = isOfferingDraw getOrElse false,
    lastDrawOffer = lastDrawOffer,
    user = user
  )
}

object RawDbPlayer {

  def encode(dbPlayer: DbPlayer): RawDbPlayer = {
    import dbPlayer._
    RawDbPlayer(
      id = id,
      c = color.name,
      ps = ps,
      aiLevel = aiLevel,
      w = isWinner,
      evts = evts,
      elo = elo,
      isOfferingDraw = if (isOfferingDraw) Some(true) else None,
      lastDrawOffer = lastDrawOffer,
      user = user
    )
  }
}
