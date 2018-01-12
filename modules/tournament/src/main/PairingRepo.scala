package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.collection.breakOut

import BSONHandlers._
import lila.db.dsl._
import lila.user.User

object PairingRepo {

  private lazy val coll = Env.current.pairingColl

  def selectTour(tourId: String) = $doc("tid" -> tourId)
  def selectUser(userId: String) = $doc("u" -> userId)
  private def selectTourUser(tourId: String, userId: String) = $doc(
    "tid" -> tourId,
    "u" -> userId
  )
  private val selectPlaying = $doc("s" $lt chess.Status.Mate.id)
  private val selectFinished = $doc("s" $gte chess.Status.Mate.id)
  private val recentSort = $doc("d" -> -1)
  private val chronoSort = $doc("d" -> 1)

  def byId(id: String): Fu[Option[Pairing]] = coll.find($id(id)).uno[Pairing]

  def recentByTour(tourId: String, nb: Int): Fu[Pairings] =
    coll.find(selectTour(tourId)).sort(recentSort).cursor[Pairing]().gather[List](nb)

  def lastOpponents(tourId: String, userIds: Iterable[String], nb: Int): Fu[Pairing.LastOpponents] =
    coll.find(
      selectTour(tourId) ++ $doc("u" $in userIds),
      $doc("_id" -> false, "u" -> true)
    ).sort(recentSort).cursor[Bdoc]().fold(Map.empty[String, String], nb) { (acc, doc) =>
        ~doc.getAs[List[String]]("u") match {
          case List(u1, u2) =>
            val acc1 = acc.contains(u1).fold(acc, acc.updated(u1, u2))
            acc.contains(u2).fold(acc1, acc1.updated(u2, u1))
        }
      } map Pairing.LastOpponents.apply

  def opponentsOf(tourId: String, userId: String): Fu[Set[String]] =
    coll.find(
      selectTourUser(tourId, userId),
      $doc("_id" -> false, "u" -> true)
    ).cursor[Bdoc]().gather[List]().map {
        _.flatMap { doc =>
          ~doc.getAs[List[String]]("u").find(userId!=)
        }(breakOut)
      }

  def recentIdsByTourAndUserId(tourId: String, userId: String, nb: Int): Fu[List[String]] =
    coll.find(
      selectTourUser(tourId, userId),
      $doc("_id" -> true)
    ).sort(recentSort).cursor[Bdoc]().gather[List](nb).map {
        _.flatMap(_.getAs[String]("_id"))
      }

  def byTourUserNb(tourId: String, userId: String, nb: Int): Fu[Option[Pairing]] =
    (nb > 0) ?? coll.find(
      selectTourUser(tourId, userId)
    ).sort(chronoSort).skip(nb - 1).uno[Pairing]

  def removeByTour(tourId: String) = coll.remove(selectTour(tourId)).void

  def removeByTourAndUserId(tourId: String, userId: String) =
    coll.remove(selectTourUser(tourId, userId)).void

  def count(tourId: String): Fu[Int] =
    coll.count(selectTour(tourId).some)

  private[tournament] def countByTourIdAndUserIds(tourId: String): Fu[Map[String, Int]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregate(
      Match(selectTour(tourId)),
      List(
        Project($doc("u" -> true, "_id" -> false)),
        UnwindField("u"),
        GroupField("u")("nb" -> SumValue(1))
      )
    ).map {
        _.firstBatch.flatMap { doc =>
          doc.getAs[String]("_id") flatMap { uid =>
            doc.getAs[Int]("nb") map { uid -> _ }
          }
        }(breakOut)
      }
  }

  def removePlaying(tourId: Tournament.ID) = coll.remove(selectTour(tourId) ++ selectPlaying).void

  def findPlaying(tourId: Tournament.ID): Fu[Pairings] =
    coll.find(selectTour(tourId) ++ selectPlaying).cursor[Pairing]().gather[List]()

  def findPlaying(tourId: Tournament.ID, userId: User.ID): Fu[Option[Pairing]] =
    coll.find(selectTourUser(tourId, userId) ++ selectPlaying).uno[Pairing]

  def isPlaying(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    coll.exists(selectTourUser(tourId, userId) ++ selectPlaying)

  private[tournament] def finishedByPlayerChronological(tourId: String, userId: String): Fu[Pairings] =
    coll.find(
      selectTourUser(tourId, userId) ++ selectFinished
    ).sort(chronoSort).list[Pairing]()

  def insert(pairing: Pairing) = coll.insert {
    pairingHandler.write(pairing) ++ $doc("d" -> DateTime.now)
  }.void

  def finish(g: lila.game.Game) =
    if (g.aborted) coll.remove($id(g.id)).void
    else coll.update(
      $id(g.id),
      $set(
        "s" -> g.status.id,
        "w" -> g.winnerColor.map(_.white),
        "t" -> g.turns
      )
    ).void

  def setBerserk(pairing: Pairing, userId: String) = (userId match {
    case uid if pairing.user1 == uid => "b1".some
    case uid if pairing.user2 == uid => "b2".some
    case _ => none
  }) ?? { field =>
    coll.update(
      $id(pairing.id),
      $set(field -> true)
    ).void
  }

  private[tournament] def playingUserIds(tour: Tournament): Fu[Set[String]] =
    coll.distinct[String, Set]("u", Some(selectTour(tour.id) ++ selectPlaying))

  private[tournament] def rawStats(tourId: String): Fu[List[Bdoc]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregate(
      Match(selectTour(tourId)),
      List(
        Project($doc(
          "_id" -> false,
          "w" -> true,
          "t" -> true,
          "b1" -> $doc("$cond" -> $arr("$b1", 1, 0)),
          "b2" -> $doc("$cond" -> $arr("$b2", 1, 0))
        )),
        GroupField("w")(
          "games" -> SumValue(1),
          "moves" -> SumField("t"),
          "b1" -> SumField("b1"),
          "b2" -> SumField("b2")
        )
      )
    ).map { _.firstBatch }
  }
}
