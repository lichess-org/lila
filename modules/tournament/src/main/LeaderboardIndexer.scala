package lila.tournament

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.core.tournament.leaderboard.Ratio
import lila.db.dsl.{ *, given }

final private class LeaderboardIndexer(
    pairingRepo: PairingRepo,
    playerRepo: PlayerRepo,
    leaderboardRepo: LeaderboardRepo
)(using Executor):

  import LeaderboardApi.*
  import BSONHandlers.given

  // def generateAll: Funit =
  //   leaderboardRepo.coll.delete.one($empty) >>
  //     tournamentRepo.coll
  //       .find(tournamentRepo.finishedSelect)
  //       .sort($sort desc "startsAt")
  //       .cursor[Tournament](ReadPref.sec)
  //       .documentSource()
  //       .via(lila.common.LilaStream.logRate[Tournament]("leaderboard index tour")(logger))
  //       .mapAsyncUnordered(1)(generateTourEntries)
  //       .mapConcat(identity)
  //       .via(lila.common.LilaStream.logRate[Entry]("leaderboard index entries")(logger))
  //       .grouped(500)
  //       .mapAsyncUnordered(1)(saveEntries)
  //       .run()
  //       .void

  def indexOne(tour: Tournament): Funit =
    (leaderboardRepo.coll.delete.one($doc("t" -> tour.id)) >>
      generateTourEntries(tour)).flatMap(saveEntries)

  private def saveEntries(entries: Seq[Entry]): Funit =
    entries.nonEmpty.so(leaderboardRepo.coll.insert.many(entries).void)

  private def generateTourEntries(tour: Tournament): Fu[List[Entry]] =
    for
      nbGames <- pairingRepo.countByTourIdAndUserIds(tour.id)
      players <- playerRepo.bestByTourWithRank(tour.id, nb = 9000, skip = 0)
    yield players.flatMap { case RankedPlayer(rank, player) =>
      nbGames.get(player.userId).map { nb =>
        Entry(
          id = player._id,
          tourId = tour.id,
          userId = player.userId,
          nbGames = nb,
          score = player.score,
          rank = rank,
          rankRatio = Ratio(if tour.nbPlayers > 0 then rank.value.toDouble / tour.nbPlayers else 0),
          freq = tour.scheduleFreq,
          perf = tour.perfType,
          date = tour.startsAt
        )
      }
    }
