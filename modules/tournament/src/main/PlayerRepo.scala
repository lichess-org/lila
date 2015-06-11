package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

object PlayerRepo {

  private lazy val coll = Env.current.playerColl

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectTour(tourId: String) = BSONDocument("tid" -> tourId)
  private val selectActive = BSONDocument("w" -> BSONDocument("$ne" -> true))
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

  def rankedByTourAndUserIds(tourId: String, userIds: Seq[String]): Fu[RankedPlayers] =
    coll.find(selectTour(tourId) ++ BSONDocument(
      "uid" -> BSONDocument("$in" -> userIds)
    )).cursor[Player].collect[List]() flatMap rankPlayers(tourId)_

  def updateRanks(tourId: String): Funit =
    coll.find(
      selectTour(tourId), 
      BSONDocument("_id" -> true)
    ).sort(bestSort).cursor[BSONDocument].collect[List] flatMap { objs =>
      _.flatMap

  private def rankPlayers(tourId: String)(players: List[Player]): Fu[RankedPlayers] =
    coll.db command Count(coll.name, Some {
      selectTour(tourId) ++ BSONDocument("m" -> BSONDocument(
        "$lt" -> players.foldLeft(0) {
          case (m, p) => if (p.magicScore > m) p.magicScore else m
        }))
    }) map { before =>
  players.foldRight(List.empty[RankedPlayer] -> before) {
    case (p, (res, rank)) => RankedPlayer([[
    }
}
