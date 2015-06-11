package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import BSONHandlers._
import lila.db.BSON._
import lila.db.Implicits._

object PairingRepo {

  private lazy val coll = Env.current.pairingColl

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectTour(tourId: String) = BSONDocument("tid" -> tourId)
  private def selectUser(userId: String) = BSONDocument("u" -> userId)
  private val selectPlaying = BSONDocument("s" -> BSONDocument("$lt" -> chess.Status.Mate.id))
  private val selectFinished = BSONDocument("s" -> BSONDocument("$gte" -> chess.Status.Mate.id))
  private val recentSort = BSONDocument("d" -> -1)
  private val chronoSort = BSONDocument("d" -> 1)

  def byId(id: String): Fu[Option[Pairing]] = coll.find(selectId(id)).one[Pairing]

  def recentByTour(tourId: String, nb: Int): Fu[List[Pairing]] =
    coll.find(selectTour(tourId)).sort(recentSort).cursor[Pairing].collect[List](nb)

  def countByTour(tour: Tournament): Fu[Int] =
    coll.db command Count(coll.name, selectTour(tour.id).some)

  def finishedByPlayerChronological(tourId: String, userId: String): Fu[List[Pairing]] =
    coll.find(
      selectTour(tourId) ++ selectUser(userId) ++ selectFinished
    ).sort(chronoSort).cursor[Pairing].collect[List]()

  def playingUserIds(tour: Tournament): Fu[Set[String]] =
    coll.db.command(Aggregate(coll.name, Seq(
      Match(selectTour(tour.id) ++ selectPlaying),
      Project("u" -> BSONBoolean(true), "_id" -> BSONBoolean(false)),
      Unwind("u"),
      Group(BSONBoolean(true))("ids" -> AddToSet("u"))
    ))) map {
      _.headOption flatMap {
        _.getAs[Set[String]]("ids")
      } getOrElse Set.empty
    }
}
