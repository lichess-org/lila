package lila.swiss

import chess.Color
import lila.game.Game
import lila.user.User

case class SwissPairing(
    id: Game.ID,
    swissId: Swiss.Id,
    round: SwissRound.Number,
    white: User.ID,
    black: User.ID,
    status: SwissPairing.Status
) {
  def apply(c: Color)             = c.fold(white, black)
  def gameId                      = id
  def players                     = List(white, black)
  def has(userId: User.ID)        = white == userId || black == userId
  def colorOf(userId: User.ID)    = chess.Color.fromWhite(white == userId)
  def opponentOf(userId: User.ID) = if (white == userId) black else white
  def winner: Option[User.ID]     = (~status.toOption).map(apply)
  def isOngoing                   = status.isLeft
  def resultFor(userId: User.ID)  = winner.map(userId.==)
  def whiteWins                   = status == Right(Some(Color.White))
  def blackWins                   = status == Right(Some(Color.Black))
  def isDraw                      = status == Right(None)
  def strResultOf(color: Color)   = status.fold(_ => "*", _.fold("1/2")(c => if (c == color) "1" else "0"))
}

object SwissPairing {

  sealed trait Ongoing
  case object Ongoing extends Ongoing
  type Status = Either[Ongoing, Option[Color]]

  val ongoing: Status = Left(Ongoing)

  case class Pending(
      white: User.ID,
      black: User.ID
  )
  case class Bye(player: User.ID)

  type ByeOrPending = Either[Bye, Pending]

  type PairingMap = Map[User.ID, Map[SwissRound.Number, SwissPairing]]

  case class View(pairing: SwissPairing, player: SwissPlayer.WithUser)

  object Fields {
    val id      = "_id"
    val swissId = "s"
    val round   = "r"
    val gameId  = "g"
    val players = "p"
    val status  = "t"
  }
  def fields[A](f: Fields.type => A): A = f(Fields)

  def toMap(pairings: List[SwissPairing]): PairingMap =
    pairings.foldLeft[PairingMap](Map.empty) { case (acc, pairing) =>
      pairing.players.foldLeft(acc) { case (acc, player) =>
        acc.updatedWith(player) { acc =>
          (~acc).updated(pairing.round, pairing).some
        }
      }
    }
}
