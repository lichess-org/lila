package lila.swiss

import lila.db.dsl._

final class SwissScoring(
    colls: SwissColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def recompute(swiss: Swiss): Funit = {
    for {
      prevPlayers <- fetchPlayers(swiss)
      pairings    <- fetchPairings(swiss)
      pairingMap = SwissPairing.toMap(pairings)
      playersWithPoints = prevPlayers.map { player =>
        val playerPairings = ~pairingMap.get(player.number)
        player.copy(
          points = swiss.allRounds.foldLeft(Swiss.Points(0)) {
            case (points, round) =>
              points + playerPairings.get(round).fold(Swiss.Points(1)) { pairing =>
                pairing.status match {
                  case Right(Some(winner)) if winner == player.number => Swiss.Points(2)
                  case Right(None)                                    => Swiss.Points(1)
                  case _                                              => Swiss.Points(0)
                }
              }
          }
        )
      }
      playerMap = SwissPlayer.toMap(playersWithPoints)
      players = playersWithPoints.map { p =>
        val playerPairings = (~pairingMap.get(p.number)).values
        val (tieBreak, perfSum) = playerPairings.foldLeft(0f -> 0f) {
          case ((tieBreak, perfSum), pairing) =>
            val opponent       = playerMap.get(pairing opponentOf p.number)
            val opponentPoints = opponent.??(_.points.value)
            val result         = pairing.winner.map(p.number.==)
            val newTieBreak    = tieBreak + result.fold(opponentPoints / 2) { _ ?? opponentPoints }
            val newPerf = perfSum + opponent.??(_.rating) + result.?? { win =>
              if (win) 500 else -500
            }
            newTieBreak -> newPerf
        }
        p.copy(
            tieBreak = Swiss.TieBreak(tieBreak),
            performance = playerPairings.nonEmpty option Swiss.Performance(perfSum / playerPairings.size)
          )
          .recomputeScore
      }
      _ <- SwissPlayer.fields { f =>
        prevPlayers
          .zip(players)
          .map {
            case (prev, player) =>
              val upd = (prev.points != player.points).?? { $doc(f.points -> player.points) } ++
                (prev.score != player.score).?? { $doc(f.score -> player.score) }
              (!upd.isEmpty) ?? colls.player.update.one($id(player.id), $set(upd)).void
          }
          .sequenceFu
          .void
      }
    } yield {}
  }.monSuccess(_.swiss.tiebreakRecompute)

  private def fetchPlayers(swiss: Swiss) = SwissPlayer.fields { f =>
    colls.player.ext
      .find($doc(f.swissId -> swiss.id))
      .sort($sort asc f.number)
      .list[SwissPlayer]()
  }

  private def fetchPairings(swiss: Swiss) = SwissPairing.fields { f =>
    colls.pairing.ext
      .find($doc(f.swissId -> swiss.id))
      .list[SwissPairing]()
  }
}
