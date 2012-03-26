package lila.system
package model

import com.novus.salat.annotations._

import lila.chess._
import Pos.{ posAt, piotr }
import Role.forsyth

case class RawDbGame(
    @Key("_id") id: String,
    players: List[RawDbPlayer],
    pgn: String,
    status: Int,
    turns: Int,
    clock: Option[RawDbClock],
    lastMove: Option[String],
    check: Option[String],
    creatorColor: String = "white",
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false,
    variant: Int = 1) {

  def decode: Option[DbGame] = for {
    whitePlayer ← players find (_.color == "white") flatMap (_.decode)
    blackPlayer ← players find (_.color == "black") flatMap (_.decode)
    trueStatus ← Status(status)
    trueCreatorColor ← Color(creatorColor)
    trueVariant ← Variant(variant)
    validClock = clock flatMap (_.decode)
    if validClock.isDefined == clock.isDefined
  } yield DbGame(
    id = id,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    pgn = pgn,
    status = trueStatus,
    turns = turns,
    clock = validClock,
    lastMove = lastMove,
    check = check flatMap posAt,
    creatorColor = trueCreatorColor,
    positionHashes = positionHashes,
    castles = castles,
    isRated = isRated,
    variant = trueVariant
  )
}

object RawDbGame {

  def encode(dbGame: DbGame): RawDbGame = {
    import dbGame._
    RawDbGame(
      id = id,
      players = players map RawDbPlayer.encode,
      pgn = pgn,
      status = status.id,
      turns = turns,
      clock = clock map RawDbClock.encode,
      lastMove = lastMove,
      check = check map (_.key),
      creatorColor = creatorColor.name,
      positionHashes = positionHashes,
      castles = castles,
      isRated = isRated,
      variant = variant.id
    )
  }
}
