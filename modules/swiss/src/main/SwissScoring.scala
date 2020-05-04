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
        p.copy(score = Swiss.Score {
          (~pairingMap.get(p.number)).values.foldLeft(0d) {
            case (score, pairing) =>
              def opponentPoints = playerMap.get(pairing opponentOf p.number).??(_.points.value)
              score + pairing.winner.map(p.number.==).fold(opponentPoints / 2) { _ ?? opponentPoints }
          }
        })
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
