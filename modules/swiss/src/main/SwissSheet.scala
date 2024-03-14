package lila.swiss

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }

import BsonHandlers.given

private case class SwissSheet(outcomes: List[SwissSheet.Outcome]):
  import SwissSheet.*

  def points = SwissSheet.pointsFor(outcomes)

  def pointsAfterRound(round: SwissRoundNumber) = SwissSheet.pointsFor(outcomes.take(round.value))

private object SwissSheet:

  case class OfPlayer private (player: SwissPlayer, sheet: SwissSheet)
  object OfPlayer:
    def withSheetPoints(player: SwissPlayer, sheet: SwissSheet): OfPlayer =
      OfPlayer(player.copy(points = sheet.points), sheet)

  enum Outcome:
    case Bye
    case Late // missed the first round
    case Absent
    case Ongoing
    case Win
    case Loss
    case Draw
    case ForfeitLoss
    case ForfeitWin

  import Outcome.*

  def pointsFor(outcomes: List[Outcome]): SwissPoints = SwissPoints.fromDoubled:
    outcomes.foldLeft(0): (acc, out) =>
      acc + pointsFor(out).doubled

  def pointsFor(outcome: Outcome): SwissPoints = SwissPoints.fromDoubled:
    outcome match
      case Win | Bye | ForfeitWin => 2
      case Late | Draw            => 1
      case _                      => 0

  def many(
      swiss: Swiss,
      players: List[SwissPlayer],
      pairingMap: SwissPairing.PairingMap
  ): List[SwissSheet] =
    many(swiss.allRounds, players, pairingMap)

  def many(
      rounds: List[SwissRoundNumber],
      players: List[SwissPlayer],
      pairingMap: SwissPairing.PairingMap
  ): List[SwissSheet] =
    players.map: player =>
      one(rounds, ~pairingMap.get(player.userId), player)

  def one(
      swiss: Swiss,
      pairingMap: Map[SwissRoundNumber, SwissPairing],
      player: SwissPlayer
  ): SwissSheet = one(swiss.allRounds, pairingMap, player)

  def one(
      rounds: List[SwissRoundNumber],
      pairingMap: Map[SwissRoundNumber, SwissPairing],
      player: SwissPlayer
  ): SwissSheet =
    SwissSheet:
      rounds.map: round =>
        pairingMap.get(round) match
          case Some(pairing) =>
            pairing.status match
              case Left(_)     => Ongoing
              case Right(None) => Draw
              case Right(Some(color)) if pairing.isForfeit =>
                if pairing(color) == player.userId then ForfeitWin else ForfeitLoss
              case Right(Some(color)) => if pairing(color) == player.userId then Win else Loss
          case None if player.byes(round) => Bye
          case None if round.value == 1   => Late
          case None                       => Absent

final private class SwissSheetApi(mongo: SwissMongo)(using
    Executor,
    akka.stream.Materializer
):

  def source(
      swiss: Swiss,
      sort: Bdoc
  ): Source[(SwissPlayer, Map[SwissRoundNumber, SwissPairing], SwissSheet), ?] =
    val readPref: ReadPref =
      if swiss.finishedAt.exists(_.isBefore(nowInstant.minusSeconds(10)))
      then _.priTemp
      else _.pri
    SwissPlayer
      .fields: f =>
        mongo.player.find($doc(f.swissId -> swiss.id)).sort(sort)
      .cursor[SwissPlayer](readPref)
      .documentSource()
      .mapAsync(4): player =>
        SwissPairing.fields: f =>
          mongo.pairing
            .list[SwissPairing](
              $doc(f.swissId -> swiss.id, f.players -> player.userId),
              readPref
            )
            .dmap(player -> _)
      .map: (player, pairings) =>
        val pairingMap = pairings.mapBy(_.round)
        (player, pairingMap, SwissSheet.one(swiss, pairingMap, player))
