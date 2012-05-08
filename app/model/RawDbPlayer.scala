package lila
package model

import chess._

import com.mongodb.DBRef

case class RawDbPlayer(
    id: String,
    c: String,
    ps: String,
    aiLevel: Option[Int],
    w: Option[Boolean],
    elo: Option[Int],
    isOfferingDraw: Option[Boolean],
    lastDrawOffer: Option[Int],
    isOfferingTakeback: Option[Boolean],
    user: Option[DBRef],
    mts: Option[String],
    blurs: Option[Int]) {

  def decode: Option[DbPlayer] = for {
    trueColor ← Color(c)
  } yield DbPlayer(
    id = id,
    color = trueColor,
    ps = ps,
    aiLevel = aiLevel,
    isWinner = w,
    elo = elo,
    isOfferingDraw = isOfferingDraw getOrElse false,
    lastDrawOffer = lastDrawOffer,
    isOfferingTakeback = isOfferingTakeback getOrElse false,
    user = user,
    moveTimes = mts | "",
    blurs = blurs | 0
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
      elo = elo,
      isOfferingDraw = if (isOfferingDraw) Some(true) else None,
      lastDrawOffer = lastDrawOffer,
      isOfferingTakeback = if (isOfferingTakeback) Some(true) else None,
      user = user,
      mts = Some(moveTimes) filter ("" !=),
      blurs = Some(blurs) filter (0 !=)
    )
  }
}
