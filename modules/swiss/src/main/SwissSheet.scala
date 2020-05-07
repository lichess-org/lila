package lila.swiss

private case class SwissSheet(outcomes: List[SwissSheet.Outcome]) {
  import SwissSheet._
  def points =
    Swiss.Points {
      outcomes.foldLeft(0) { case (acc, out) => acc + pointsFor(out) }
    }
}

private object SwissSheet {

  sealed trait Outcome
  case object Bye     extends Outcome
  case object Late    extends Outcome // missed the first round
  case object Absent  extends Outcome
  case object Ongoing extends Outcome
  case object Win     extends Outcome
  case object Loss    extends Outcome
  case object Draw    extends Outcome

  def pointsFor(outcome: Outcome) =
    outcome match {
      case Win               => 2
      case Late | Bye | Draw => 1
      case _                 => 0
    }

  def many(
      swiss: Swiss,
      players: List[SwissPlayer],
      pairingMap: SwissPairing.PairingMap
  ): List[SwissSheet] =
    players.map { player =>
      one(swiss, ~pairingMap.get(player.number), player)
    }

  def one(
      swiss: Swiss,
      pairingMap: Map[SwissRound.Number, SwissPairing],
      player: SwissPlayer
  ): SwissSheet =
    SwissSheet {
      swiss.allRounds.map { round =>
        pairingMap get round match {
          case Some(pairing) =>
            pairing.status match {
              case Left(_)                              => Ongoing
              case Right(None)                          => Draw
              case Right(Some(n)) if n == player.number => Win
              case Right(_)                             => Loss
            }
          case None if player.byes(round) => Bye
          case None if round.value == 1   => Late
          case None                       => Absent
        }
      }
    }
}
