package lila.tournament

import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.dsl._

private final class LeaderboardIndexer(
    tournamentColl: Coll,
    leaderboardColl: Coll
) {

  import LeaderboardApi._
  import BSONHandlers._

  def generateAll: Funit = leaderboardColl.remove($empty) >> {
    import reactivemongo.play.iteratees.cursorProducer

    tournamentColl.find(TournamentRepo.finishedSelect)
      .sort($sort desc "startsAt")
      .cursor[Tournament]().enumerator(20 * 1000) &>
      Enumeratee.mapM[Tournament].apply[Seq[Entry]](generateTour) &>
      Enumeratee.mapConcat[Seq[Entry]].apply[Entry](identity) &>
      Enumeratee.grouped(Iteratee takeUpTo 500) |>>>
      Iteratee.foldM[Seq[Entry], Int](0) {
        case (number, entries) =>
          if (number % 10000 == 0)
            logger.info(s"Generating leaderboards... $number")
          saveEntries("-")(entries) inject (number + entries.size)
      }
  }.void

  def indexOne(tour: Tournament): Funit =
    leaderboardColl.remove($doc("t" -> tour.id)) >>
      generateTour(tour) flatMap saveEntries(tour.id)

  private def saveEntries(tourId: String)(entries: Seq[Entry]): Funit =
    entries.nonEmpty ?? leaderboardColl.bulkInsert(
      documents = entries.map(BSONHandlers.leaderboardEntryHandler.write).toStream,
      ordered = false
    ).void

  private def generateTour(tour: Tournament): Fu[List[Entry]] = for {
    nbGames <- PairingRepo.countByTourIdAndUserIds(tour.id)
    players <- PlayerRepo.bestByTourWithRank(tour.id, nb = 9000, skip = 0)
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
      date = tour.startsAt
    )
  }
}
