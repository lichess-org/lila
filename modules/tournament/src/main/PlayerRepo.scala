package lila.tournament

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Descending, Group, Match, PushField, Sort, AvgField }
import reactivemongo.api.{ CursorProducer, Cursor, ReadPreference }
import reactivemongo.bson._

import BSONHandlers._
import lila.common.MaxPerSecond
import lila.db.dsl._
import lila.hub.lightTeam.TeamId
import lila.rating.Perf
import lila.user.{ User, Perfs }

object PlayerRepo {

  private lazy val coll = Env.current.playerColl

  private def selectId(id: Tournament.ID) = $doc("_id" -> id)
  private def selectTour(tourId: Tournament.ID) = $doc("tid" -> tourId)
  private def selectTourUser(tourId: Tournament.ID, userId: User.ID) = $doc(
    "tid" -> tourId,
    "uid" -> userId
  )
  private val selectActive = $doc("w" $ne true)
  private val selectWithdraw = $doc("w" -> true)
  private val bestSort = $doc("m" -> -1)

  def byId(id: Tournament.ID): Fu[Option[Player]] = coll.uno[Player](selectId(id))

  private[tournament] def bestByTour(tourId: Tournament.ID, nb: Int, skip: Int = 0): Fu[List[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).skip(skip).list[Player](nb)

  private[tournament] def bestByTourWithRank(tourId: Tournament.ID, nb: Int, skip: Int = 0): Fu[RankedPlayers] =
    bestByTour(tourId, nb, skip).map { res =>
      res.foldRight(List.empty[RankedPlayer] -> (res.size + skip)) {
        case (p, (res, rank)) => (RankedPlayer(rank, p) :: res, rank - 1)
      }._1
    }

  private[tournament] def bestByTourWithRankByPage(tourId: Tournament.ID, nb: Int, page: Int): Fu[RankedPlayers] =
    bestByTourWithRank(tourId, nb, (page - 1) * nb)

  // very expensive
  private[tournament] def bestTeamIdsByTour(tourId: Tournament.ID, battle: TeamBattle): Fu[List[TeamBattle.RankedTeam]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    import TeamBattle.{ RankedTeam, TopPlayer }
    coll.aggregateList(
      Match(selectTour(tourId)),
      List(
        Sort(Descending("m")),
        GroupField("t")("m" -> Push($doc(
          "u" -> "$uid",
          "m" -> "$m"
        ))),
        Project($doc(
          "p" -> $doc(
            "$slice" -> $arr("$m", battle.nbTopPlayers)
          )
        ))
      ),
      maxDocs = 10
    ).map {
        _.flatMap { doc =>
          doc.getAs[TeamId]("_id") flatMap { teamId =>
            doc.getAs[List[Bdoc]]("p") map {
              _.flatMap {
                case p: Bdoc => for {
                  id <- p.getAs[User.ID]("u")
                  magic <- p.getAs[Int]("m")
                } yield TopPlayer(id, magic)
              }
            } map { RankedTeam(0, teamId, _) }
          }
        }.sortBy(-_.magicScore).zipWithIndex map {
          case (rt, pos) => rt.copy(rank = pos + 1)
        }
      } map { ranked =>
        if (ranked.size == battle.teams.size) ranked
        else ranked ::: battle.teams.foldLeft(List.empty[RankedTeam]) {
          case (missing, team) if !ranked.exists(_.teamId == team) =>
            RankedTeam(missing.headOption.fold(ranked.size)(_.rank) + 1, team, Nil) :: missing
          case (acc, _) => acc
        }.reverse
      }
  }

  def countActive(tourId: Tournament.ID): Fu[Int] =
    coll.countSel(selectTour(tourId) ++ selectActive)

  def count(tourId: Tournament.ID): Fu[Int] = coll.countSel(selectTour(tourId))

  def removeByTour(tourId: Tournament.ID) = coll.remove(selectTour(tourId)).void

  def remove(tourId: Tournament.ID, userId: User.ID) =
    coll.remove(selectTourUser(tourId, userId)).void

  def filterExists(tourIds: List[Tournament.ID], userId: User.ID): Fu[List[Tournament.ID]] =
    coll.primitive[Tournament.ID]($doc(
      "tid" $in tourIds,
      "uid" -> userId
    ), "tid")

  def existsActive(tourId: Tournament.ID, userId: User.ID) =
    coll.exists(selectTourUser(tourId, userId) ++ selectActive)

  def unWithdraw(tourId: Tournament.ID) = coll.update(
    selectTour(tourId) ++ selectWithdraw,
    $doc("$unset" -> $doc("w" -> true)),
    multi = true
  ).void

  def find(tourId: Tournament.ID, userId: User.ID): Fu[Option[Player]] =
    coll.find(selectTourUser(tourId, userId)).uno[Player]

  def update(tourId: Tournament.ID, userId: User.ID)(f: Player => Fu[Player]) =
    find(tourId, userId) flatten s"No such player: $tourId/$userId" flatMap f flatMap { player =>
      coll.update(selectId(player._id), player).void
    }

  def join(tourId: Tournament.ID, user: User, perfLens: Perfs => Perf, team: Option[TeamId]) =
    find(tourId, user.id) flatMap {
      case Some(p) if p.withdraw => coll.update(selectId(p._id), $unset("w"))
      case Some(p) => funit
      case None => coll.insert(Player.make(tourId, user, perfLens, team))
    } void

  def withdraw(tourId: Tournament.ID, userId: User.ID) =
    coll.update(selectTourUser(tourId, userId), $set("w" -> true)).void

  private[tournament] def withPoints(tourId: Tournament.ID): Fu[List[Player]] =
    coll.find(
      selectTour(tourId) ++ $doc("m" $gt 0)
    ).list[Player]()

  private[tournament] def userIds(tourId: Tournament.ID): Fu[List[User.ID]] =
    coll.distinct[User.ID, List]("uid", selectTour(tourId).some)

  private[tournament] def activeUserIds(tourId: Tournament.ID): Fu[List[User.ID]] =
    coll.distinct[User.ID, List](
      "uid", (selectTour(tourId) ++ selectActive).some
    )

  def winner(tourId: Tournament.ID): Fu[Option[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).uno[Player]

  // freaking expensive (marathons)
  private[tournament] def computeRanking(tourId: Tournament.ID): Fu[Ranking] =
    coll.aggregateOne(Match(selectTour(tourId)), List(
      Sort(Descending("m")),
      Group(BSONNull)("uids" -> PushField("uid"))
    )) map {
      _ ?? {
        _ get "uids" match {
          case Some(BSONArray(uids)) =>
            // mutable optimized implementation
            val b = Map.newBuilder[User.ID, Int]
            var r = 0
            for (u <- uids) {
              b += (u.get.asInstanceOf[BSONString].value -> r)
              r = r + 1
            }
            b.result
          case _ => Map.empty
        }
      }
    }

  def computeRankOf(player: Player): Fu[Int] =
    coll.countSel(selectTour(player.tourId) ++ $doc("m" $gt player.magicScore))

  // expensive, cache it
  private[tournament] def averageRating(tourId: Tournament.ID): Fu[Int] =
    coll.aggregateOne(Match(selectTour(tourId)), List(
      Group(BSONNull)("rating" -> AvgField("r"))
    )) map {
      ~_.flatMap(_.getAs[Double]("rating").map(_.toInt))
    }

  def byTourAndUserIds(tourId: Tournament.ID, userIds: Iterable[User.ID]): Fu[List[Player]] =
    coll.find(selectTour(tourId) ++ $doc("uid" $in userIds))
      .list[Player]()
      .chronometer.logIfSlow(200, logger) { players =>
        s"PlayerRepo.byTourAndUserIds $tourId ${userIds.size} user IDs, ${players.size} players"
      }.result

  def pairByTourAndUserIds(tourId: Tournament.ID, id1: User.ID, id2: User.ID): Fu[Option[(Player, Player)]] =
    byTourAndUserIds(tourId, List(id1, id2)) map {
      case List(p1, p2) if p1.is(id1) && p2.is(id2) => Some(p1 -> p2)
      case List(p1, p2) if p1.is(id2) && p2.is(id1) => Some(p2 -> p1)
      case _ => none
    }

  def setPerformance(player: Player, performance: Int) =
    coll.update(selectId(player.id), $doc("$set" -> $doc("e" -> performance))).void

  private def rankPlayers(players: List[Player], ranking: Ranking): RankedPlayers =
    players.flatMap { p =>
      ranking get p.userId map { RankedPlayer(_, p) }
    }.sortBy(_.rank)

  def rankedByTourAndUserIds(tourId: Tournament.ID, userIds: Iterable[User.ID], ranking: Ranking): Fu[RankedPlayers] =
    byTourAndUserIds(tourId, userIds).map { rankPlayers(_, ranking) }
      .chronometer
      .logIfSlow(200, logger) { players =>
        s"PlayerRepo.rankedByTourAndUserIds $tourId ${userIds.size} user IDs, ${ranking.size} ranking, ${players.size} players"
      }.result

  def searchPlayers(tourId: Tournament.ID, term: String, nb: Int): Fu[List[User.ID]] =
    User.couldBeUsername(term) ?? {
      term.nonEmpty ?? coll.primitive[User.ID](
        selector = $doc(
          "tid" -> tourId,
          "uid" $startsWith term.toLowerCase
        ),
        sort = $sort desc "m",
        nb = nb,
        field = "uid"
      )
    }

  private[tournament] def cursor(
    tournamentId: Tournament.ID,
    batchSize: Int,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cp: CursorProducer[Player]): cp.ProducedCursor = {
    val query = coll
      .find(selectTour(tournamentId))
      .sort($sort desc "m")
    query.copy(options = query.options.batchSize(batchSize)).cursor[Player](readPreference)
  }
}
