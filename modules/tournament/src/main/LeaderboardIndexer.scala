package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.Types.Coll

private final class LeaderboardIndexer(
    tournamentColl: Coll,
    leaderboardColl: Coll) {

  import LeaderboardApi._
  import BSONHandlers._

  def generateAll: Funit = leaderboardColl.remove(BSONDocument()) >> {
    tournamentColl.find(TournamentRepo.finishedSelect)
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament]()
      .enumerate(20 * 1000, stopOnError = true) &>
      Enumeratee.mapM[Tournament].apply[Seq[Entry]](generateTour) &>
      Enumeratee.mapConcat[Seq[Entry]].apply[Entry](identity) &>
      Enumeratee.grouped(Iteratee takeUpTo 500) |>>>
      Iteratee.foldM[Seq[Entry], Int](0) {
        case (number, entries) =>
          if (number % 10000 == 0)
            logger.info(s"Generating leaderboards... $number")
          saveEntries(entries) inject (number + entries.size)
      }
  }.void

  def indexOne(tour: Tournament): Funit =
    leaderboardColl.remove(BSONDocument("t" -> tour.id)) >>
      generateTour(tour) flatMap saveEntries

  private def saveEntries(entries: Seq[Entry]) =
    entries.nonEmpty ?? leaderboardColl.bulkInsert(
      documents = entries.map(BSONHandlers.leaderboardEntryHandler.write).toStream,
      ordered = false
    ).void

  private def generateTour(tour: Tournament): Fu[List[Entry]] = for {
    nbGames <- PairingRepo.countByTourIdAndUserIds(tour.id)
    players <- PlayerRepo.bestByTourWithRank(tour.id, nb = 5000, skip = 0)
  } yield players.flatMap {
    case RankedPlayer(rank, player) => for {
      perfType <- tour.perfType
      nb <- nbGames get player.userId
    } yield Entry(
      id = player._id,
      tourId = tour.id,
      userId = player.userId,
      nbGames = nb,
      score = player.score,
      rank = rank,
      rankRatio = Ratio(if (tour.nbPlayers > 0) rank.toDouble / tour.nbPlayers else 0),
      freq = tour.schedule.map(_.freq),
      speed = tour.schedule.map(_.speed),
      perf = perfType,
      date = tour.startsAt)
  }
}
