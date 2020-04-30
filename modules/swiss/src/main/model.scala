package lila.swiss

import lila.user.User
import lila.game.Game

case class SwissPlayer(
    _id: SwissPlayer.Id, // random
    swissId: Swiss.Id,
    number: SwissPlayer.Number,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    points: Swiss.Points,
    score: Swiss.Score
)

object SwissPlayer {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)

  case class Number(value: Int) extends AnyVal with IntValue
}

object SwissRound {

  case class Number(value: Int) extends AnyVal with IntValue
}

case class SwissPairing(
    _id: Game.ID,
    swissId: Swiss.Id,
    round: SwissRound.Number,
    white: SwissPlayer.Number,
    black: SwissPlayer.Number,
    winner: Option[SwissPlayer.Number]
) {
  def gameId                                 = _id
  def players                                = List(white, black)
  def has(number: SwissPlayer.Number)        = white == number || black == number
  def colorOf(number: SwissPlayer.Number)    = chess.Color(white == number)
  def opponentOf(number: SwissPlayer.Number) = if (white == number) black else white
}

object SwissPairing {

  case class Pending(
      white: SwissPlayer.Number,
      black: SwissPlayer.Number
  )
}

case class SwissBye(
    round: SwissRound.Number,
    player: SwissPlayer.Number
)

case class LeaderboardPlayer(
    player: SwissPlayer,
    pairings: Map[SwissRound.Number, SwissPairing]
)

case class MyInfo(rank: Int, withdraw: Boolean, gameId: Option[Game.ID]) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}
