package lila.swiss

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class SwissScoring(mongo: SwissMongo)(using Scheduler, Executor):

  import BsonHandlers.given

  def compute(id: SwissId): Fu[Option[SwissScoring.Result]] = sequencer(id).monSuccess(_.swiss.scoringGet)

  private val sequencer = scalalib.actor.AskPipelines[SwissId, Option[SwissScoring.Result]](
    compute = recompute,
    expiration = 1.minute,
    timeout = 10.seconds,
    name = "swiss.scoring"
  )

  private def recompute(id: SwissId): Fu[Option[SwissScoring.Result]] =
    mongo.swiss
      .byId[Swiss](id)
      .flatMap:
        _.so { swiss =>
          for
            (prevPlayers, pairings) <- fetchPlayers(swiss).zip(fetchPairings(swiss))
            pairingMap = SwissPairing.toMap(pairings)
            sheets     = SwissSheet.many(swiss, prevPlayers, pairingMap)
            withSheets = prevPlayers.zip(sheets).map(SwissSheet.OfPlayer.withSheetPoints)
            players    = SwissScoring.computePlayers(swiss.round, withSheets, pairingMap)
            _ <- SwissPlayer.fields: f =>
              prevPlayers
                .zip(players)
                .withFilter(_ != _)
                .map: (_, player) =>
                  mongo.player.update
                    .one(
                      $id(player.id),
                      $set(
                        f.points      -> player.points,
                        f.tieBreak    -> player.tieBreak,
                        f.performance -> player.performance,
                        f.score       -> player.score
                      )
                    )
                .parallelVoid
          yield SwissScoring
            .Result(
              swiss,
              players.zip(sheets).sortBy(-_._1.score.value),
              players.mapBy(_.userId),
              pairingMap
            )
            .some
        }.monSuccess(_.swiss.scoringRecompute)

  private def fetchPlayers(swiss: Swiss) =
    SwissPlayer.fields: f =>
      mongo.player
        .find($doc(f.swissId -> swiss.id))
        .sort($sort.asc(f.score))
        .cursor[SwissPlayer]()
        .listAll()

  private def fetchPairings(swiss: Swiss) =
    (!swiss.isCreated).so:
      SwissPairing.fields: f =>
        mongo.pairing.list[SwissPairing]($doc(f.swissId -> swiss.id))

private object SwissScoring:

  case class Result(
      swiss: Swiss,
      leaderboard: List[(SwissPlayer, SwissSheet)],
      playerMap: SwissPlayer.PlayerMap,
      pairings: SwissPairing.PairingMap
  ):
    def countOngoingPairings: Int = leaderboard
      .collect:
        case (player, _) if player.present => player.userId
      .flatMap(pairings.get)
      .flatMap(_.get(swiss.round))
      .filter(_.isOngoing)
      .map(_.gameId)
      .distinct
      .size

  def computePlayers(
      rounds: SwissRoundNumber,
      playerSheets: List[SwissSheet.OfPlayer],
      pairingMap: SwissPairing.PairingMap
  ) =
    val playerMap = playerSheets.map(_.player).mapBy(_.userId)
    playerSheets.map:
      case SwissSheet.OfPlayer(player, playerSheet) =>
        val playerPairings: Map[SwissRoundNumber, SwissPairing] =
          (~pairingMap.get(player.userId)).values.mapBy(_.round)
        val pairingsAndByes: List[(SwissRoundNumber, Option[SwissPairing | SwissPairing.Bye])] =
          SwissRoundNumber
            .from((1 to rounds.value).toList)
            .map: round =>
              round -> {
                if player.byes(round) then SwissPairing.Bye(player.userId).some
                else playerPairings.get(round)
              }
        val (tieBreak, perfSum) = pairingsAndByes.foldLeft(0f -> 0f):
          case ((tieBreak, perfSum), (round, Some(pairing: SwissPairing))) =>
            val opponent       = playerMap.get(pairing.opponentOf(player.userId))
            val opponentPoints = opponent.so(_.points.value)
            val result         = pairing.resultFor(player.userId)
            val newTieBreak    = tieBreak + result.fold(opponentPoints / 2)(_.so(opponentPoints))
            val newPerf = perfSum + opponent.so(_.rating.value) + result.so:
              if _ then 500 else -500
            newTieBreak -> newPerf
          case ((tieBreak, perfSum), (round, Some(_: SwissPairing.Bye))) =>
            /* https://handbook.fide.com/files/handbook/C02Standards.pdf
            For tie-break purposes a player who has no opponent will be
            considered as having played against a virtual opponent who has
            the same number of points at the beginning of the round and
            who draws in all the following rounds. For the round itself the
            result by forfeit will be considered as a normal result. */
            val virtualOpponentOutcomes = SwissSheet.Outcome.ForfeitLoss ::
              List.fill((rounds - round).value)(SwissSheet.Outcome.Draw)
            val pointsOfVirtualOpponent =
              playerSheet.pointsAfterRound(round - 1) + SwissSheet.pointsFor(virtualOpponentOutcomes)
            val newTieBreak = tieBreak + pointsOfVirtualOpponent.value
            newTieBreak -> perfSum
          case ((tieBreak, perfSum), (round, None)) =>
            tieBreak -> perfSum

        player
          .copy(
            tieBreak = Swiss.TieBreak(tieBreak),
            performance = playerPairings.nonEmpty.option(Swiss.Performance(perfSum / playerPairings.size))
          )
          .recomputeScore
