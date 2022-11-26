package lila.swiss

import akka.stream.scaladsl.*
import BsonHandlers.given
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }

private case class SwissSheet(outcomes: List[SwissSheet.Outcome]):
  import SwissSheet.*

  def points = SwissPoints fromDouble {
    outcomes.foldLeft(0) { case (acc, out) => acc + pointsFor(out).doubled }
  }

private object SwissSheet:

  sealed trait Outcome
  case object Bye         extends Outcome
  case object Late        extends Outcome // missed the first round
  case object Absent      extends Outcome
  case object Ongoing     extends Outcome
  case object Win         extends Outcome
  case object Loss        extends Outcome
  case object Draw        extends Outcome
  case object ForfeitLoss extends Outcome
  case object ForfeitWin  extends Outcome

  def pointsFor(outcome: Outcome) = SwissPoints fromDouble {
    outcome match
      case Win | Bye | ForfeitWin => 2
      case Late | Draw            => 1
      case _                      => 0
  }

  def many(
      swiss: Swiss,
      players: List[SwissPlayer],
      pairingMap: SwissPairing.PairingMap
  ): List[SwissSheet] =
    players.map { player =>
      one(swiss, ~pairingMap.get(player.userId), player)
    }

  def one(
      swiss: Swiss,
      pairingMap: Map[SwissRoundNumber, SwissPairing],
      player: SwissPlayer
  ): SwissSheet =
    SwissSheet {
      swiss.allRounds.map { round =>
        pairingMap get round match
          case Some(pairing) =>
            pairing.status match
              case Left(_)     => Ongoing
              case Right(None) => Draw
              case Right(Some(color)) if pairing.isForfeit =>
                if (pairing(color) == player.userId) ForfeitWin else ForfeitLoss
              case Right(Some(color)) => if (pairing(color) == player.userId) Win else Loss
          case None if player.byes(round) => Bye
          case None if round.value == 1   => Late
          case None                       => Absent
      }
    }

final private class SwissSheetApi(mongo: SwissMongo)(using
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
):

  def source(
      swiss: Swiss,
      sort: Bdoc
  ): Source[(SwissPlayer, Map[SwissRoundNumber, SwissPairing], SwissSheet), ?] =
    val readPreference =
      if (swiss.finishedAt.exists(_ isBefore DateTime.now.minusSeconds(10)))
        temporarilyPrimary
      else ReadPreference.primary
    SwissPlayer
      .fields { f =>
        mongo.player.find($doc(f.swissId -> swiss.id)).sort(sort)
      }
      .cursor[SwissPlayer](readPreference)
      .documentSource()
      .mapAsync(4) { player =>
        SwissPairing.fields { f =>
          mongo.pairing.list[SwissPairing](
            $doc(f.swissId -> swiss.id, f.players -> player.userId),
            readPreference
          ) dmap { player -> _ }
        }
      }
      .map { case (player, pairings) =>
        val pairingMap = pairings.map { p =>
          p.round -> p
        }.toMap
        (player, pairingMap, SwissSheet.one(swiss, pairingMap, player))
      }
