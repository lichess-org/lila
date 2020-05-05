package lila.swiss

import lila.game.Game

case class SwissPairing(
    id: Game.ID,
    swissId: Swiss.Id,
    round: SwissRound.Number,
    white: SwissPlayer.Number,
    black: SwissPlayer.Number,
    status: SwissPairing.Status
) {
  def gameId                                 = id
  def players                                = List(white, black)
  def has(number: SwissPlayer.Number)        = white == number || black == number
  def colorOf(number: SwissPlayer.Number)    = chess.Color(white == number)
  def opponentOf(number: SwissPlayer.Number) = if (white == number) black else white
  def winner: Option[SwissPlayer.Number]     = ~status.toOption
  def isOngoing                              = status.isLeft
  def resultFor(number: SwissPlayer.Number)  = winner.map(number.==)
}

object SwissPairing {

  sealed trait Ongoing
  case object Ongoing extends Ongoing
  type Status = Either[Ongoing, Option[SwissPlayer.Number]]

  val ongoing: Status = Left(Ongoing)

  case class Pending(
      white: SwissPlayer.Number,
      black: SwissPlayer.Number
  )
  case class Bye(player: SwissPlayer.Number)

  type ByeOrPending = Either[Bye, Pending]

  type PairingMap = Map[SwissPlayer.Number, Map[SwissRound.Number, SwissPairing]]

  case class View(pairing: SwissPairing, player: SwissPlayer.WithUser)

  object Fields {
    val id      = "_id"
    val swissId = "s"
    val round   = "r"
    val gameId  = "g"
    val players = "p"
    val status  = "t"
    val date    = "d"
  }
  def fields[A](f: Fields.type => A): A = f(Fields)

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
