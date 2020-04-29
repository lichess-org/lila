package lila.swiss

import org.joda.time.{ DateTime, Duration, Interval }
import ornicar.scalalib.Random
import chess.Clock.{ Config => ClockConfig }
import chess.StartingPosition

import lila.user.User
import lila.game.Game

case class Swiss(
    _id: Swiss.Id,
    name: String,
    status: Status,
    clock: ClockConfig,
    variant: chess.variant.Variant,
    position: StartingPosition,
    rated: Boolean,
    nbRounds: Int,
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    startsAt: DateTime,
    winnerId: Option[User.ID] = None,
    description: Option[String] = None,
    hasChat: Boolean = true
) {}

object Swiss {

  case class Id(value: String) extends AnyVal with StringValue
  case class Round(value: Int) extends AnyVal with IntValue

  case class Points(double: Int) extends AnyVal {
    def value: Float = double / 2
  }

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)
}

case class SwissPlayer(
    id: SwissPlayer.Id,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    points: Swiss.Points
) {
  def number = id.number
}

object SwissPlayer {

  case class Id(swissId: Swiss.Id, number: Number)

  case class Number(value: Int) extends AnyVal with IntValue
}

case class SwissRound(
    id: SwissRound.Id,
    pairings: List[SwissPairing]
    // byes: List[SwissPlayer.Number]
) {
  def number = id.number
  val pairingsMap: Map[SwissPlayer.Number, SwissPairing] = pairings.view.flatMap { p =>
    List(
      p.white -> p,
      p.black -> p
    )
  }.toMap
}

object SwissRound {

  case class Id(swissId: Swiss.Id, number: Number) {
    override def toString = s"$swissId:$number"
  }

  case class Number(value: Int) extends AnyVal with IntValue
}

case class SwissPairing(
    gameId: Game.ID,
    white: SwissPlayer.Number,
    black: SwissPlayer.Number,
    winner: Option[SwissPlayer.Number]
) {
  def colorOf(number: SwissPlayer.Number)    = chess.Color(white == number)
  def opponentOf(number: SwissPlayer.Number) = if (white == number) black else white
}

object SwissPairing {

  case class Id(value: String) extends AnyVal with StringValue

  case class Pending(
      white: SwissPlayer.Number,
      black: SwissPlayer.Number
  )
}
