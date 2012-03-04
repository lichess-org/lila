package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

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
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false) {

  def decode: Option[DbGame] = for {
    player1 ← players.lift(0) flatMap (_.decode)
    player2 ← players.lift(1) flatMap (_.decode)
    validPlayers = List(player1, player2)
    colors = validPlayers map (_.color)
    //if (colors.pp contains White) && (colors contains Black)
    validClock = clock flatMap (_.decode)
    if validClock.isDefined == clock.isDefined
  } yield DbGame(
    id = id,
    players = validPlayers,
    pgn = pgn,
    status = status,
    turns = turns,
    clock = validClock,
    lastMove = lastMove,
    positionHashes = positionHashes,
    castles = castles,
    isRated = isRated
  )
}

object RawDbGame {

  def encode(dbGame: DbGame): RawDbGame = {
    import dbGame._
    RawDbGame(
      id = id,
      players = players map RawDbPlayer.encode,
      pgn = pgn,
      status = status,
      turns = turns,
      clock = clock map RawDbClock.encode,
      lastMove = lastMove,
      positionHashes = positionHashes,
      castles = castles,
      isRated = isRated
    )
  }

  def encode(clock: Clock): RawDbClock = {
    import clock._
    RawDbClock(
      color = color.name,
      increment = increment,
      limit = limit,
      times = Map(
        "white" -> whiteTime,
        "black" -> blackTime
      )
    )
  }
}
