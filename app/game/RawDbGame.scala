package lila
package game

import com.novus.salat.annotations._

import chess._
import Pos.{ posAt, piotr }
import Role.forsyth

import org.joda.time.DateTime
import com.mongodb.DBRef

case class RawDbGame(
    @Key("_id") id: String,
    players: List[RawDbPlayer],
    pgn: Option[String],
    status: Int,
    turns: Int,
    clock: Option[RawDbClock],
    lastMove: Option[String],
    check: Option[String],
    cc: String = "white",
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false,
    v: Int = 1,
    next: Option[DBRef],
    lmt: Option[Int] = None,
    createdAt: Option[DateTime],
    updatedAt: Option[DateTime]) {

  def decode: Option[DbGame] = for {
    whitePlayer ← players find (_.c == "white") flatMap (_.decode)
    blackPlayer ← players find (_.c == "black") flatMap (_.decode)
    trueStatus ← Status(status)
    trueCreatorColor ← Color(cc)
    trueVariant ← Variant(v)
    validClock = clock flatMap (_.decode)
    if validClock.isDefined == clock.isDefined
  } yield DbGame(
    id = id,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    pgn = pgn | "",
    status = trueStatus,
    turns = turns,
    clock = validClock,
    lastMove = lastMove,
    check = check flatMap posAt,
    creatorColor = trueCreatorColor,
    positionHashes = positionHashes,
    castles = castles,
    isRated = isRated,
    variant = trueVariant,
    next = next,
    lastMoveTime = lmt,
    createdAt = createdAt,
    updatedAt = updatedAt
  )
}

object RawDbGame {

  def encode(dbGame: DbGame): RawDbGame = {
    import dbGame._
    RawDbGame(
      id = id,
      players = players map RawDbPlayer.encode,
      pgn = Some(pgn),
      status = status.id,
      turns = turns,
      clock = clock map RawDbClock.encode,
      lastMove = lastMove,
      check = check map (_.key),
      cc = creatorColor.name,
      positionHashes = positionHashes,
      castles = castles,
      isRated = isRated,
      v = variant.id,
      next = next,
      lmt = lastMoveTime,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }
}
