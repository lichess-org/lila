package lila.swiss

import chess.Color
import lila.game.Game
import lila.user.User

case class SwissPairing(
    id: Game.ID,
    swissId: Swiss.Id,
    round: SwissRound.Number,
    sente: User.ID,
    gote: User.ID,
    status: SwissPairing.Status
) {
  def apply(c: Color)             = c.fold(sente, gote)
  def gameId                      = id
  def players                     = List(sente, gote)
  def has(userId: User.ID)        = sente == userId || gote == userId
  def colorOf(userId: User.ID)    = chess.Color(sente == userId)
  def opponentOf(userId: User.ID) = if (sente == userId) gote else sente
  def winner: Option[User.ID]     = (~status.toOption).map(apply)
  def isOngoing                   = status.isLeft
  def resultFor(userId: User.ID)  = winner.map(userId.==)
  def senteWins                   = status == Right(Some(Color.Sente))
  def goteWins                   = status == Right(Some(Color.Gote))
  def isDraw                      = status == Right(None)
}

object SwissPairing {

  sealed trait Ongoing
  case object Ongoing extends Ongoing
  type Status = Either[Ongoing, Option[Color]]

  val ongoing: Status = Left(Ongoing)

  case class Pending(
      sente: User.ID,
      gote: User.ID
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
    pairings.foldLeft[PairingMap](Map.empty) {
      case (acc, pairing) =>
        pairing.players.foldLeft(acc) {
          case (acc, player) =>
            acc.updatedWith(player) { acc =>
              (~acc).updated(pairing.round, pairing).some
            }
        }
    }
}
