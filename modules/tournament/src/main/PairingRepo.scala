package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

object PairingRepo {

  private lazy val coll = Env.current.pairingColl

  private def selectId(id: String) = BSONDocument("_id" -> id)
  def selectTour(tourId: String) = BSONDocument("tid" -> tourId)
  def selectUser(userId: String) = BSONDocument("u" -> userId)
  private def selectTourUser(tourId: String, userId: String) = BSONDocument(
    "tid" -> tourId,
    "u" -> userId)
  private val selectPlaying = BSONDocument("s" -> BSONDocument("$lt" -> chess.Status.Mate.id))
  private val selectFinished = BSONDocument("s" -> BSONDocument("$gte" -> chess.Status.Mate.id))
  private val recentSort = BSONDocument("d" -> -1)
  private val chronoSort = BSONDocument("d" -> 1)

  def byId(id: String): Fu[Option[Pairing]] = coll.find(selectId(id)).one[Pairing]

  def recentByTour(tourId: String, nb: Int): Fu[Pairings] =
    coll.find(selectTour(tourId)).sort(recentSort).cursor[Pairing]().collect[List](nb)

  def lastOpponents(tourId: String, userIds: Iterable[String], nb: Int): Fu[Pairing.LastOpponents] =
    coll.find(
      selectTour(tourId) ++ BSONDocument("u" -> BSONDocument("$in" -> userIds)),
      BSONDocument("_id" -> false, "u" -> true)
    ).sort(recentSort).cursor[BSONDocument]().enumerate(nb) |>>>
      Iteratee.fold(scala.collection.immutable.Map.empty[String, String]) { (acc, doc) =>
        ~doc.getAs[List[String]]("u") match {
          case List(u1, u2) =>
            val acc1 = acc.contains(u1).fold(acc, acc.updated(u1, u2))
            acc.contains(u2).fold(acc1, acc1.updated(u2, u1))
        }
      } map Pairing.LastOpponents.apply

  def opponentsOf(tourId: String, userId: String): Fu[Set[String]] =
    coll.find(
      selectTourUser(tourId, userId),
      BSONDocument("_id" -> false, "u" -> true)
    ).cursor[BSONDocument]().collect[List]().map {
        _.flatMap { doc =>
          ~doc.getAs[List[String]]("u").filter(userId!=)
        }.toSet
      }

  def recentIdsByTourAndUserId(tourId: String, userId: String, nb: Int): Fu[List[String]] =
    coll.find(
      selectTourUser(tourId, userId),
      BSONDocument("_id" -> true)
    ).sort(recentSort).cursor[BSONDocument]().collect[List](nb).map {
        _.flatMap(_.getAs[String]("_id"))
      }

  def byTourUserNb(tourId: String, userId: String, nb: Int): Fu[Option[Pairing]] =
    (nb > 0) ?? coll.find(
      selectTourUser(tourId, userId)
    ).sort(chronoSort).skip(nb - 1).one[Pairing]

  def removeByTour(tourId: String) = coll.remove(selectTour(tourId)).void

  def removeByTourAndUserId(tourId: String, userId: String) =
    coll.remove(selectTourUser(tourId, userId)).void

  def count(tourId: String): Fu[Int] =
    coll.count(selectTour(tourId).some)

  def countByTourIdAndUserIds(tourId: String): Fu[Map[String, Int]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregate(
      Match(selectTour(tourId)),
      List(
        Project(BSONDocument("u" -> true, "_id" -> false)),
        Unwind("u"),
        GroupField("u")("nb" -> SumValue(1))
      )).map {
        _.documents.flatMap { doc =>
          doc.getAs[String]("_id") flatMap { uid =>
            doc.getAs[Int]("nb") map { uid -> _ }
          }
        }.toMap
      }
  }

  def removePlaying(tourId: String) = coll.remove(selectTour(tourId) ++ selectPlaying).void

  def findPlaying(tourId: String): Fu[Pairings] =
    coll.find(selectTour(tourId) ++ selectPlaying).cursor[Pairing]().collect[List]()

  def findPlaying(tourId: String, userId: String): Fu[Option[Pairing]] =
    coll.find(selectTourUser(tourId, userId) ++ selectPlaying).one[Pairing]

  def finishedByPlayerChronological(tourId: String, userId: String): Fu[Pairings] =
    coll.find(
      selectTourUser(tourId, userId) ++ selectFinished
    ).sort(chronoSort).cursor[Pairing]().collect[List]()

  def insert(pairing: Pairing) = coll.insert {
    pairingHandler.write(pairing) ++ BSONDocument("d" -> DateTime.now)
  }.void

  def finish(g: lila.game.Game) =
    if (g.aborted) coll.remove(selectId(g.id))
    else coll.update(
      selectId(g.id),
      BSONDocument("$set" -> BSONDocument(
        "s" -> g.status.id,
        "w" -> g.winnerColor.map(_.white),
        "t" -> g.turns))).void

  def setBerserk(pairing: Pairing, userId: String, value: Int) = (userId match {
    case uid if pairing.user1 == uid => "b1".some
    case uid if pairing.user2 == uid => "b2".some
    case _                           => none
  }) ?? { field =>
    coll.update(
      selectId(pairing.id),
      BSONDocument("$set" -> BSONDocument(field -> value))).void
  }

  import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework, AggregationFramework.{ AddToSet, Group, Match, Project, Push, Unwind }

  def playingUserIds(tour: Tournament): Fu[Set[String]] =
    coll.aggregate(Match(selectTour(tour.id) ++ selectPlaying), List(
      Project(BSONDocument(
        "u" -> BSONBoolean(true), "_id" -> BSONBoolean(false))),
      Unwind("u"), Group(BSONBoolean(true))("ids" -> AddToSet("u")))).map(
      _.documents.headOption.flatMap(_.getAs[Set[String]]("ids")).
        getOrElse(Set.empty[String]))

  def playingGameIds(tourId: String): Fu[List[String]] =
    coll.aggregate(Match(selectTour(tourId) ++ selectPlaying), List(
      Group(BSONBoolean(true))("ids" -> Push("_id")))).map(
      _.documents.headOption.flatMap(_.getAs[List[String]]("ids")).
        getOrElse(List.empty[String]))
}
