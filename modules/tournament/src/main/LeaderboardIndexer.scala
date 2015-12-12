package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.variant.Variant
import lila.db.BSON._
import lila.db.Types.Coll

private final class LeaderboardIndexer(
    tournamentColl: Coll,
    leaderboardColl: Coll) {

  import LeaderboardApi._
  import BSONHandlers._

  def generateAll: Funit = leaderboardColl.remove(BSONDocument()) >> {
    tournamentColl.find(TournamentRepo.finishedSelect ++ TournamentRepo.scheduledSelect)
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament]()
      .enumerate(20 * 1000, stopOnError = true) &>
      Enumeratee.mapM[Tournament].apply[Seq[Entry]](generateTour) &>
      Enumeratee.mapConcat[Seq[Entry]].apply[Entry](identity) &>
      Enumeratee.grouped(Iteratee takeUpTo 500) |>>>
      Iteratee.foldM[Seq[Entry], Int](0) {
        case (number, entries) =>
          if (number % 10000 == 0)
            play.api.Logger("tournament").info(s"Generating leaderboards... $number")
          leaderboardColl.bulkInsert(
            documents = entries.map(BSONHandlers.leaderboardEntryHandler.write).toStream,
            ordered = false) inject (number + entries.size)
      }
  }.void

  private def generateTour(tour: Tournament): Fu[List[Entry]] =
    tour.schedule ?? { sched =>
      PairingRepo.countByTourIdAndUserIds(tour.id) flatMap { nbGames =>
        PlayerRepo.bestByTourWithRank(tour.id, nb = 5000, skip = 0) map {
          _.flatMap {
            case RankedPlayer(rank, player) =>
              nbGames get player.userId map { nbGames =>
                val weightedRank = rank * 10000 - tour.nbPlayers
                Entry(
                  id = player._id,
                  tourId = tour.id,
                  userId = player.userId,
                  nbGames = nbGames,
                  score = player.score,
                  rank = rank,
                  weightedRank = weightedRank,
                  freq = sched.freq,
                  speed = sched.speed,
                  variant = tour.variant,
                  date = tour.startsAt)
              }
          }
        }
      }
    }
}
