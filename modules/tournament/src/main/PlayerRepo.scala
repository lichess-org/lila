package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._
import lila.rating.Perf
import lila.user.{ User, Perfs }

object PlayerRepo {

  private lazy val coll = Env.current.playerColl

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectTour(tourId: String) = BSONDocument("tid" -> tourId)
  private def selectUser(userId: String) = BSONDocument("uid" -> userId)
  private val selectActive = BSONDocument("w" -> BSONDocument("$ne" -> true))
  private val selectWithdraw = BSONDocument("w" -> true)
  private val bestSort = BSONDocument("m" -> -1)

  def byId(id: String): Fu[Option[Player]] = coll.find(selectId(id)).one[Player]

  def bestByTour(tourId: String, nb: Int): Fu[List[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).cursor[Player].collect[List](nb)

  def bestByTourWithRank(tourId: String, nb: Int): Fu[RankedPlayers] =
    bestByTour(tourId, nb).map {
      _.foldRight(List.empty[RankedPlayer] -> 1) {
        case (p, (res, rank)) => (RankedPlayer(rank, p) :: res, rank + 1)
      }._1
    }

  def countActive(tourId: String): Fu[Int] =
    coll.db command Count(coll.name, Some(selectTour(tourId) ++ selectActive))

  def count(tourId: String): Fu[Int] =
    coll.db command Count(coll.name, Some(selectTour(tourId)))

  def removeByTour(tourId: String) = coll.remove(selectTour(tourId)).void

  def remove(tourId: String, userId: String) =
    coll.remove(selectTour(tourId) ++ selectUser(userId)).void

  def existsActive(tourId: String, userId: String) =
    coll.db command Count(coll.name, Some(
      selectTour(tourId) ++ selectUser(userId) ++ selectActive
    )) map (0!=)

  def unWithdraw(tourId: String) = coll.update(
    selectTour(tourId) ++ selectWithdraw,
    BSONDocument("$unset" -> BSONDocument("w" -> true)),
    multi = true).void

  def find(tourId: String, userId: String): Fu[Option[Player]] =
    coll.find(selectTour(tourId) ++ selectUser(userId)).one[Player]

  def join(tourId: String, user: User, perfLens: Perfs => Perf) =
    find(tourId, user.id) flatMap {
      case Some(p) if p.withdraw => coll.update(selectId(p.id), BSONDocument("$unset" -> BSONDocument("w" -> true)))
      case Some(p)               => funit
      case None                  => coll.insert(Player.make(tourId, user, perfLens))
    } void

  def withdraw(tourId: String, userId: String) = coll.update(
    selectTour(tourId) ++ selectUser(userId),
    BSONDocument("w" -> true)).void

  def withPoints(tourId: String): Fu[List[Player]] =
    coll.find(
      selectTour(tourId) ++ BSONDocument("m" -> BSONDocument("$gt" -> 0))
    ).cursor[Player].collect[List]()

  private def aggregationUserIdList(res: Stream[BSONDocument]): List[String] =
    res.headOption flatMap { _.getAs[List[String]]("uids") } getOrElse Nil

  def userIds(tourId: String): Fu[List[String]] =
    coll.db.command(Aggregate(coll.name, Seq(
      Match(selectTour(tourId)),
      Group(BSONBoolean(true))("uids" -> Push("uid"))
    ))) map aggregationUserIdList

  def activeUserIds(tourId: String): Fu[List[String]] =
    coll.db.command(Aggregate(coll.name, Seq(
      Match(selectTour(tourId) ++ selectActive),
      Group(BSONBoolean(true))("uids" -> Push("uid"))
    ))) map aggregationUserIdList

  def winner(tourId: String): Fu[Option[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).one[Player]

  def ranking(tourId: String): Fu[Ranking] =
    coll.db.command(Aggregate(coll.name, Seq(
      Match(selectTour(tourId)),
      Sort(Seq(Descending("m"))),
      Group(BSONBoolean(true))("uids" -> Push("uid"))
    ))) map aggregationUserIdList map { _.zipWithIndex.toMap }

  def byTourAndUserIds(tourId: String, userIds: Iterable[String]): Fu[List[Player]] =
    coll.find(selectTour(tourId) ++ BSONDocument(
      "uid" -> BSONDocument("$in" -> userIds)
    )).cursor[Player].collect[List]()

  def rankPlayers(players: List[Player], ranking: Ranking): RankedPlayers =
    players.flatMap { p =>
      ranking get p.userId map { RankedPlayer(_, p) }
    }.sortBy(-_.rank)

  def rankedByTourAndUserIds(tourId: String, userIds: Iterable[String], ranking: Ranking): Fu[RankedPlayers] =
    byTourAndUserIds(tourId, userIds) map { rankPlayers(_, ranking) }
}
