package lila.tournament

// import akka.stream.Materializer
// import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson._

import BSONHandlers._
import lila.db.dsl._
import lila.game.Game
import lila.user.User

final class ArrangementRepo(coll: Coll)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private def selectTour(tourId: Tournament.ID) = $doc("t" -> tourId)
  private def selectUser(userId: User.ID)       = $doc("u" -> userId)

  private def selectTourUser(tourId: Tournament.ID, userId: User.ID) =
    $doc(
      "t" -> tourId,
      "u" -> userId
    )
  private def selectTourUsers(tourId: Tournament.ID, user1Id: User.ID, user2Id: User.ID) =
    $doc(
      "t" -> tourId,
      "u" $all List(user1Id, user2Id)
    )
  // to hit tour index
  private def selectTourGame(tourId: Tournament.ID, gameId: Game.ID) =
    $doc(
      "t" -> tourId,
      "g" -> gameId
    )

  private val selectPlaying  = $doc("s" $lt shogi.Status.Mate.id)
  private val selectWithGame = $doc("g" $exists true)

  private def afterNow   = $doc("d" $gt DateTime.now.minusMinutes(30)) // some reserve
  private val recentSort = $doc("d" -> -1)
  private val chronoSort = $doc("d" -> 1)

  def byId(id: Arrangement.ID): Fu[Option[Arrangement]] = coll.byId[Arrangement](id)

  def byLookup(lookup: Arrangement.Lookup): Fu[Option[Arrangement]] =
    coll.one[Arrangement](selectTourUsers(lookup.tourId, lookup.users._1, lookup.users._2) ++ {
      lookup.order.filter(_ > 0) ?? { case o => $doc("o" -> o) }
    })

  def byGame(tourId: Tournament.ID, gameId: Game.ID): Fu[Option[Arrangement]] =
    coll.one[Arrangement](selectTourGame(tourId, gameId))

  def allByTour(tourId: Tournament.ID): Fu[List[Arrangement]] =
    coll.list[Arrangement](selectTour(tourId))

  def havePlayedTogether(tourId: Tournament.ID, user1Id: User.ID, user2Id: User.ID): Fu[Boolean] =
    coll.exists(selectTourUsers(tourId, user1Id, user2Id))

  def removePlaying(tourId: Tournament.ID) = coll.delete.one(selectTour(tourId) ++ selectPlaying).void

  def findPlaying(tourId: Tournament.ID, userId: User.ID): Fu[List[Arrangement]] =
    coll.list[Arrangement](selectTourUser(tourId, userId) ++ selectPlaying)

  def isPlaying(tourId: Tournament.ID, userId: User.ID): Fu[Boolean] =
    coll.exists(selectTourUser(tourId, userId) ++ selectPlaying)

  def countWithGame(tourId: Tournament.ID): Fu[Int] =
    coll.countSel(selectTour(tourId) ++ selectWithGame)

  def update(arrangement: Arrangement): Funit =
    coll.update.one($id(arrangement.id), arrangement, upsert = true).void

  def withGame(id: Arrangement.ID, gid: lila.game.Game.ID) =
    coll.update
      .one(
        $id(id),
        $set(
          "g" -> gid
        ) ++ $unset("r1", "r2", "d1", "d2", "ua")
      )
      .void

  def finish(tour: Tournament, g: lila.game.Game) =
    if (g.aborted)
      coll.update
        .one(
          selectTourGame(tour.id, g.id),
          $unset("g")
        )
        .void
    else
      coll.update
        .one(
          selectTourGame(tour.id, g.id),
          $set(
            "s" -> g.status.id,
            "w" -> g.winnerUserId,
            "p" -> g.plies
          )
        )
        .void

  def removeByTour(tourId: Tournament.ID) = coll.delete.one(selectTour(tourId)).void

  private[tournament] def allUpcomingByUserIdChronological(
      userId: User.ID
  ): Fu[List[Arrangement]] =
    coll.ext
      .find(
        selectUser(userId) ++ afterNow
      )
      .sort(chronoSort)
      .cursor[Arrangement]()
      .list()

  def recentGameIdsByTourAndUserId(tourId: Tournament.ID, userId: User.ID, nb: Int): Fu[List[Tournament.ID]] =
    coll
      .find(
        selectTourUser(tourId, userId),
        $doc("g" -> true).some
      )
      .sort(recentSort)
      .cursor[Bdoc]()
      .list(nb)
      .dmap {
        _.flatMap(_.getAsOpt[Game.ID]("g"))
      }

  private[tournament] def countByTourIdAndUserIds(tourId: Tournament.ID): Fu[Map[User.ID, Int]] = {
    val max = 10_000
    coll
      .aggregateList(maxDocs = max) { framework =>
        import framework._
        Match(selectTour(tourId)) -> List(
          Project($doc("u" -> true, "_id" -> false)),
          UnwindField("u"),
          GroupField("u")("nb" -> SumAll),
          Sort(Descending("nb")),
          Limit(max)
        )
      }
      .map {
        _.view
          .flatMap { doc =>
            doc.getAsOpt[User.ID]("_id") flatMap { uid =>
              doc.int("nb") map { uid -> _ }
            }
          }
          .toMap
      }
  }

  private[tournament] def rawStats(tourId: Tournament.ID): Fu[List[Bdoc]] = {
    coll.aggregateList(maxDocs = 3) { framework =>
      import framework._
      Match(selectTour(tourId)) -> List(
        Project(
          $doc(
            "_id" -> false,
            "w"   -> true,
            "p"   -> true
          )
        ),
        GroupField("w")(
          "games" -> SumAll,
          "moves" -> SumField("p")
        )
      )
    }
  }

}
