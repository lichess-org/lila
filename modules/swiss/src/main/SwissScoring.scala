package lila.swiss

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class SwissScoring(mongo: SwissMongo)(using
    scheduler: Scheduler,
    ec: Executor
):

  import BsonHandlers.given

  def apply(id: SwissId): Fu[Option[SwissScoring.Result]] = sequencer(id).monSuccess(_.swiss.scoringGet)

  private val sequencer =
    new lila.hub.AskPipelines[SwissId, Option[SwissScoring.Result]](
      compute = recompute,
      expiration = 1 minute,
      timeout = 10 seconds,
      name = "swiss.scoring"
    )

  private def recompute(id: SwissId): Fu[Option[SwissScoring.Result]] =
    mongo.swiss.byId[Swiss](id) flatMap {
      _.?? { swiss =>
        for {
          (prevPlayers, pairings) <- fetchPlayers(swiss) zip fetchPairings(swiss)
          pairingMap = SwissPairing.toMap(pairings)
          sheets     = SwissSheet.many(swiss, prevPlayers, pairingMap)
          withPoints = (prevPlayers zip sheets).map { case (player, sheet) =>
            player.copy(points = sheet.points)
          }
          playerMap = withPoints.mapBy(_.userId)
          players = withPoints.map { p =>
            val playerPairings = (~pairingMap.get(p.userId)).values
            val (tieBreak, perfSum) = playerPairings.foldLeft(0f -> 0f) {
              case ((tieBreak, perfSum), pairing) =>
                val opponent       = playerMap.get(pairing opponentOf p.userId)
                val opponentPoints = opponent.??(_.points.value)
                val result         = pairing.resultFor(p.userId)
                val newTieBreak    = tieBreak + result.fold(opponentPoints / 2) { _ ?? opponentPoints }
                val newPerf = perfSum + opponent.??(_.rating.value) + result.?? { win =>
                  if (win) 500 else -500
                }
                newTieBreak -> newPerf
            }
            p.copy(
              tieBreak = Swiss.TieBreak(tieBreak),
              performance = playerPairings.nonEmpty option Swiss.Performance(perfSum / playerPairings.size)
            ).recomputeScore
          }
          _ <- SwissPlayer.fields { f =>
            prevPlayers
              .zip(players)
              .withFilter { case (a, b) =>
                a != b
              }
              .map { case (_, player) =>
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
                  .void
              }
              .parallel
              .void
          }
        } yield SwissScoring
          .Result(
            swiss,
            players.zip(sheets).sortBy(-_._1.score.value),
            players.mapBy(_.userId),
            pairingMap
          )
          .some
      }.monSuccess(_.swiss.scoringRecompute)
    }

  private def fetchPlayers(swiss: Swiss) =
    SwissPlayer.fields { f =>
      mongo.player
        .find($doc(f.swissId -> swiss.id))
        .sort($sort asc f.score)
        .cursor[SwissPlayer]()
        .listAll()
    }

  private def fetchPairings(swiss: Swiss) =
    !swiss.isCreated ?? SwissPairing.fields { f =>
      mongo.pairing.list[SwissPairing]($doc(f.swissId -> swiss.id))
    }

private object SwissScoring:

  case class Result(
      swiss: Swiss,
      leaderboard: List[(SwissPlayer, SwissSheet)],
      playerMap: SwissPlayer.PlayerMap,
      pairings: SwissPairing.PairingMap
  )
