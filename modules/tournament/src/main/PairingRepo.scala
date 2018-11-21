package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.api.{ CursorProducer, ReadPreference }
import scala.collection.breakOut

import BSONHandlers._
import lila.db.dsl._
import lila.game.Game
import lila.user.User

object PairingRepo {

  private lazy val coll = Env.current.pairingColl

  def selectTour(tourId: Tournament.ID) = $doc("tid" -> tourId)
  def selectUser(userId: User.ID) = $doc("u" -> userId)
  private def selectTourUser(tourId: Tournament.ID, userId: User.ID) = $doc(
    "tid" -> tourId,
    "u" -> userId
  )
  private val selectPlaying = $doc("s" $lt chess.Status.Mate.id)
  private val selectFinished = $doc("s" $gte chess.Status.Mate.id)
  private val recentSort = $doc("d" -> -1)
  private val chronoSort = $doc("d" -> 1)

  def byId(id: Tournament.ID): Fu[Option[Pairing]] = coll.find($id(id)).uno[Pairing]

  def recentByTour(tourId: Tournament.ID, nb: Int): Fu[Pairings] =
    coll.find(selectTour(tourId)).sort(recentSort).list[Pairing](nb)

  def lastOpponents(tourId: Tournament.ID, userIds: Iterable[User.ID], nb: Int): Fu[Pairing.LastOpponents] = coll.find(
    selectTour(tourId) ++ $doc("u" $in userIds),
    $doc("_id" -> false, "u" -> true)
  ).sort(recentSort).cursor[Bdoc]().fold(Map.empty[User.ID, User.ID], nb) { (acc, doc) =>
      ~doc.getAs[List[User.ID]]("u") match {
        case List(u1, u2) =>
          val acc1 = if (acc.contains(u1)) acc else acc.updated(u1, u2)
          if (acc.contains(u2)) acc1 else acc1.updated(u2, u1)
      }
    } map Pairing.LastOpponents.apply

  def opponentsOf(tourId: Tournament.ID, userId: User.ID): Fu[Set[User.ID]] = coll.find(
    selectTourUser(tourId, userId),
    $doc("_id" -> false, "u" -> true)
  ).list[Bdoc]().map {
      _.flatMap { doc =>
        ~doc.getAs[List[User.ID]]("u").find(userId!=)
      }(breakOut)
    }

  def recentIdsByTourAndUserId(tourId: Tournament.ID, userId: User.ID, nb: Int): Fu[List[Tournament.ID]] = coll.find(
    selectTourUser(tourId, userId),
    $doc("_id" -> true)
  ).sort(recentSort).list[Bdoc](nb).map {
      _.flatMap(_.getAs[Game.ID]("_id"))
    }

  def playingByTourAndUserId(tourId: Tournament.ID, userId: User.ID): Fu[Option[Game.ID]] = coll.find(
    selectTourUser(tourId, userId) ++ selectPlaying,
    $doc("_id" -> true)
  ).sort(recentSort).uno[Bdoc].map {
      _.flatMap(_.getAs[Game.ID]("_id"))
    }

  def byTourUserNb(tourId: Tournament.ID, userId: User.ID, nb: Int): Fu[Option[Pairing]] =
    (nb > 0) ?? coll.find(
      selectTourUser(tourId, userId)
    ).sort(chronoSort).skip(nb - 1).uno[Pairing]

  def removeByTour(tourId: Tournament.ID) = coll.remove(selectTour(tourId)).void

  def removeByTourAndUserId(tourId: Tournament.ID, userId: User.ID) =
    coll.remove(selectTourUser(tourId, userId)).void

  def count(tourId: Tournament.ID): Fu[Int] =
    coll.count(selectTour(tourId).some)

  private[tournament] def countByTourIdAndUserIds(tourId: Tournament.ID): Fu[Map[User.ID, Int]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match(selectTour(tourId)),
      List(
        Project($doc("u" -> true, "_id" -> false)),
        UnwindField("u"),
        GroupField("u")("nb" -> SumValue(1)),
        Sort(Descending("nb"))
      ),
      maxDocs = 10000
    ).map {
        _.flatMap { doc =>
          doc.getAs[Game.ID]("_id") flatMap { uid =>
            doc.getAs[Int]("nb") map { uid -> _ }
          }
        }(breakOut)
      }
  }

  def removePlaying(tourId: Tournament.ID) = coll.remove(selectTour(tourId) ++ selectPlaying).void

  def findPlaying(tourId: Tournament.ID): Fu[Pairings] =
    coll.find(selectTour(tourId) ++ selectPlaying).list[Pairing]()

  def findPlaying(tourId: Tournament.ID, userId: User.ID): Fu[Option[Pairing]] =
    coll.find(selectTourUser(tourId, userId) ++ selectPlaying).uno[Pairing]

  def isPlaying(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    coll.exists(selectTourUser(tourId, userId) ++ selectPlaying)

  private[tournament] def finishedByPlayerChronological(tourId: Tournament.ID, userId: User.ID): Fu[Pairings] =
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

  def setBerserk(pairing: Pairing, userId: User.ID) = (userId match {
    case uid if pairing.user1 == uid => "b1".some
    case uid if pairing.user2 == uid => "b2".some
    case _ => none
  }) ?? { field =>
    coll.update(
      $id(pairing.id),
      $set(field -> true)
    ).void
  }

  def sortedGameIdsCursor(
    tournamentId: Tournament.ID,
    batchSize: Int = 0,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cp: CursorProducer[Bdoc]) = {
    val query = coll
      .find(selectTour(tournamentId), $id(true))
      .sort(recentSort)
    query.copy(options = query.options.batchSize(batchSize)).cursor[Bdoc](readPreference)
  }

  private[tournament] def playingUserIds(tour: Tournament): Fu[Set[User.ID]] =
    coll.distinct[User.ID, Set]("u", Some(selectTour(tour.id) ++ selectPlaying))

  private[tournament] def rawStats(tourId: Tournament.ID): Fu[List[Bdoc]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
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
      ),
      maxDocs = 3
    )
  }
}
