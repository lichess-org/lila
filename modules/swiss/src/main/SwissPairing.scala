package lila.swiss

import lila.game.Game

case class SwissPairing(
    _id: Game.ID,
    swissId: Swiss.Id,
    round: SwissRound.Number,
    white: SwissPlayer.Number,
    black: SwissPlayer.Number,
    status: SwissPairing.Status
) {
  def gameId                                 = _id
  def players                                = List(white, black)
  def has(number: SwissPlayer.Number)        = white == number || black == number
  def colorOf(number: SwissPlayer.Number)    = chess.Color(white == number)
  def opponentOf(number: SwissPlayer.Number) = if (white == number) black else white
  def winner: Option[SwissPlayer.Number]     = ~status.toOption
  def isOngoing                              = status.isLeft
  def isWinFor(number: SwissPlayer.Number)   = winner has number
}

object SwissPairing {

  sealed trait Ongoing
  case object Ongoing extends Ongoing
  type Status = Either[Ongoing, Option[SwissPlayer.Number]]

  case class Pending(
      white: SwissPlayer.Number,
      black: SwissPlayer.Number
  )

  type PairingMap = Map[SwissPlayer.Number, Map[SwissRound.Number, SwissPairing]]

  // assumes that pairings are already sorted by round (probably by the DB query)
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
