package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.dsl._
import lila.rating.Perf
import lila.user.{ User, Perfs }

object PlayerRepo {

  private lazy val coll = Env.current.playerColl

  private def selectId(id: String) = $doc("_id" -> id)
  private def selectTour(tourId: String) = $doc("tid" -> tourId)
  private def selectUser(userId: String) = $doc("uid" -> userId)
  private def selectTourUser(tourId: String, userId: String) = $doc(
    "tid" -> tourId,
    "uid" -> userId)
  private val selectActive = $doc("w" $ne true)
  private val selectWithdraw = $doc("w" -> true)
  private val bestSort = $doc("m" -> -1)

  def byId(id: String): Fu[Option[Player]] = coll.uno[Player](selectId(id))

  def bestByTour(tourId: String, nb: Int, skip: Int = 0): Fu[List[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).skip(skip).cursor[Player]().gather[List](nb)

  def bestByTourWithRank(tourId: String, nb: Int, skip: Int = 0): Fu[RankedPlayers] =
    bestByTour(tourId, nb, skip).map { res =>
      res.foldRight(List.empty[RankedPlayer] -> (res.size + skip)) {
        case (p, (res, rank)) => (RankedPlayer(rank, p) :: res, rank - 1)
      }._1
    }

  def bestByTourWithRankByPage(tourId: String, nb: Int, page: Int): Fu[RankedPlayers] =
    bestByTourWithRank(tourId, nb, (page - 1) * nb)

  def countActive(tourId: String): Fu[Int] =
    coll.count(Some(selectTour(tourId) ++ selectActive))

  def count(tourId: String): Fu[Int] = coll.count(Some(selectTour(tourId)))

  def removeByTour(tourId: String) = coll.remove(selectTour(tourId)).void

  def remove(tourId: String, userId: String) =
    coll.remove(selectTourUser(tourId, userId)).void

  def exists(tourId: String, userId: String) =
    coll.count(selectTourUser(tourId, userId).some) map (0!=)

  def existsActive(tourId: String, userId: String) =
    coll.count(Some(
      selectTourUser(tourId, userId) ++ selectActive
    )) map (0!=)

  def unWithdraw(tourId: String) = coll.update(
    selectTour(tourId) ++ selectWithdraw,
    $doc("$unset" -> $doc("w" -> true)),
    multi = true).void

  def find(tourId: String, userId: String): Fu[Option[Player]] =
    coll.find(selectTourUser(tourId, userId)).uno[Player]

  def update(tourId: String, userId: String)(f: Player => Fu[Player]) =
    find(tourId, userId) flatten s"No such player: $tourId/$userId" flatMap f flatMap { player =>
      coll.update(selectId(player._id), player).void
    }

  def playerInfo(tourId: String, userId: String): Fu[Option[PlayerInfo]] = find(tourId, userId) flatMap {
    _ ?? { player =>
      coll.countSel(selectTour(tourId) ++ $doc(
        "m" -> $doc("$gt" -> player.magicScore))
      ) map { n =>
        PlayerInfo((n + 1), player.withdraw).some
      }
    }
  }

  def join(tourId: String, user: User, perfLens: Perfs => Perf) =
    find(tourId, user.id) flatMap {
      case Some(p) if p.withdraw => coll.update(selectId(p._id), $doc("$unset" -> $doc("w" -> true)))
      case Some(p)               => funit
      case None                  => coll.insert(Player.make(tourId, user, perfLens))
    } void

  def withdraw(tourId: String, userId: String) = coll.update(
    selectTourUser(tourId, userId),
    $doc("$set" -> $doc("w" -> true))).void

  def withPoints(tourId: String): Fu[List[Player]] =
    coll.find(
      selectTour(tourId) ++ $doc("m" -> $doc("$gt" -> 0))
    ).cursor[Player]().gather[List]()

  private def aggregationUserIdList(res: Stream[Bdoc]): List[String] =
    res.headOption flatMap { _.getAs[List[String]]("uids") } getOrElse Nil

  import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Descending, Group, Match, Push, Sort }

  def userIds(tourId: String): Fu[List[String]] =
    coll.distinct[String, List]("uid", selectTour(tourId).some)

  def activeUserIds(tourId: String): Fu[List[String]] =
    coll.distinct[String, List](
      "uid", (selectTour(tourId) ++ selectActive).some)

  def winner(tourId: String): Fu[Option[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).uno[Player]

  // freaking expensive (marathons)
  private[tournament] def computeRanking(tourId: String): Fu[Ranking] =
    coll.aggregate(Match(selectTour(tourId)), List(Sort(Descending("m")),
      Group(BSONNull)("uids" -> Push("uid")))) map {
      _.firstBatch.headOption.fold(Map.empty: Ranking) {
        _ get "uids" match {
          case Some(BSONArray(uids)) =>
            // mutable optimized implementation
            val b = Map.newBuilder[String, Int]
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

  def byTourAndUserIds(tourId: String, userIds: Iterable[String]): Fu[List[Player]] =
    coll.find(selectTour(tourId) ++ $doc("uid" $in userIds))
      .list[Player]()
      .chronometer.logIfSlow(200, logger) { players =>
        s"PlayerRepo.byTourAndUserIds $tourId ${userIds.size} user IDs, ${players.size} players"
      }.result

  def pairByTourAndUserIds(tourId: String, id1: String, id2: String): Fu[Option[(Player, Player)]] =
    byTourAndUserIds(tourId, List(id1, id2)) map {
      case List(p1, p2) if p1.is(id1) && p2.is(id2) => Some(p1 -> p2)
      case List(p1, p2) if p1.is(id2) && p2.is(id1) => Some(p2 -> p1)
      case _                                        => none
    }

  def setPerformance(player: Player, performance: Int) =
    coll.update(selectId(player.id), $doc("$set" -> $doc("e" -> performance))).void

  private def rankPlayers(players: List[Player], ranking: Ranking): RankedPlayers =
    players.flatMap { p =>
      ranking get p.userId map { RankedPlayer(_, p) }
    }.sortBy(_.rank)

  def rankedByTourAndUserIds(tourId: String, userIds: Iterable[String], ranking: Ranking): Fu[RankedPlayers] =
    byTourAndUserIds(tourId, userIds).map { rankPlayers(_, ranking) }
      .chronometer
      .logIfSlow(200, logger) { players =>
        s"PlayerRepo.rankedByTourAndUserIds $tourId ${userIds.size} user IDs, ${ranking.size} ranking, ${players.size} players"
      }.result
}
