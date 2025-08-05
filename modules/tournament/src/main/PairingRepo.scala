package lila.tournament

import akka.stream.Materializer
import akka.stream.scaladsl.*
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

import BSONHandlers.given

final class PairingRepo(coll: Coll)(using Executor, Materializer):

  def selectTour(tourId: TourId) = $doc("tid" -> tourId)
  def selectUser(userId: UserId) = $doc("u" -> userId)
  private def selectTourUser(tourId: TourId, userId: UserId) =
    $doc(
      "tid" -> tourId,
      "u" -> userId
    )
  private val selectPlaying = $doc("s".$lt(chess.Status.Mate.id))
  private val selectFinished = $doc("s".$gte(chess.Status.Mate.id))
  private val recentSort = $doc("d" -> -1)
  private val chronoSort = $doc("d" -> 1)

  def byId(id: GameId): Fu[Option[Pairing]] = coll.find($id(id)).one[Pairing]

  private[tournament] def lastOpponents(
      tourId: TourId,
      userIds: Set[UserId],
      max: Int
  ): Fu[Pairing.LastOpponents] =
    userIds.nonEmpty
      .so:
        val nbUsers = userIds.size
        coll
          .find(
            selectTour(tourId) ++ $doc("u".$in(userIds)),
            $doc("_id" -> false, "u" -> true).some
          )
          .sort(recentSort)
          .batchSize(20)
          .cursor[Bdoc]()
          .documentSource(max)
          .mapConcat(_.getAsOpt[List[UserId]]("u").toList)
          .scan(Map.empty[UserId, UserId]):
            case (acc, List(u1, u2)) =>
              val b1 = userIds.contains(u1)
              val b2 = !b1 || userIds.contains(u2)
              val acc1 = if !b1 || acc.contains(u1) then acc else acc.updated(u1, u2)
              if !b2 || acc.contains(u2) then acc1 else acc1.updated(u2, u1)
            case (acc, _) => acc
          .takeWhile(
            r => r.sizeIs < nbUsers,
            inclusive = true
          )
          .toMat(Sink.lastOption)(Keep.right)
          .run()
          .dmap(~_)
      .dmap(Pairing.LastOpponents.apply)

  def opponentsOf(tourId: TourId, userId: UserId): Fu[Set[UserId]] =
    coll
      .find(
        selectTourUser(tourId, userId),
        $doc("_id" -> false, "u" -> true).some
      )
      .cursor[Bdoc]()
      .listAll()
      .dmap:
        _.view
          .flatMap { doc =>
            (~doc.getAsOpt[List[UserId]]("u")).find(userId !=)
          }
          .toSet

  def recentIdsByTourAndUserId(tourId: TourId, userId: UserId, nb: Int): Fu[List[GameId]] =
    coll
      .find(
        selectTourUser(tourId, userId),
        $doc("_id" -> true).some
      )
      .sort(recentSort)
      .cursor[Bdoc]()
      .list(nb)
      .dmap:
        _.flatMap(_.getAsOpt[GameId]("_id"))

  def playingByTourAndUserId(tourId: TourId, userId: UserId): Fu[Option[GameId]] =
    coll
      .find(
        selectTourUser(tourId, userId) ++ selectPlaying,
        $doc("_id" -> true).some
      )
      .sort(recentSort)
      .one[Bdoc]
      .dmap:
        _.flatMap(_.getAsOpt[GameId]("_id"))

  def removeByTour(tourId: TourId) = coll.delete.one(selectTour(tourId)).void

  private[tournament] def forfeitByTourAndUserId(tourId: TourId, userId: UserId): Funit =
    coll
      .list[Pairing](selectTourUser(tourId, userId))
      .flatMap:
        _.withFilter(_.notLostBy(userId))
          .map: p =>
            coll.update.one(
              $id(p.id),
              $set(
                "w" -> p.colorOf(userId).map(_.black)
              )
            )
          .parallelVoid

  def count(tourId: TourId): Fu[Int] =
    coll.countSel(selectTour(tourId))

  private[tournament] def countByTourIdAndUserIds(tourId: TourId): Fu[Map[UserId, Int]] =
    val max = 10_000
    coll
      .aggregateList(maxDocs = max, _.sec): framework =>
        import framework.*
        Match(selectTour(tourId)) -> List(
          Project($doc("u" -> true, "_id" -> false)),
          UnwindField("u"),
          GroupField("u")("nb" -> SumAll),
          Sort(Descending("nb")),
          Limit(max)
        )
      .map: docs =>
        for
          doc <- docs
          uid <- doc.getAsOpt[UserId]("_id")
          nb <- doc.int("nb")
        yield (uid, nb)
      .map(_.toMap)

  def removePlaying(tourId: TourId) = coll.delete.one(selectTour(tourId) ++ selectPlaying).void

  def findPlaying(tourId: TourId, userId: UserId): Fu[Option[Pairing]] =
    coll.find(selectTourUser(tourId, userId) ++ selectPlaying).one[Pairing]

  def isRecentPlayer(tourId: TourId, userId: UserId): Fu[Boolean] =
    coll.exists:
      selectTourUser(tourId, userId) ++
        $or(selectPlaying, $doc("d".$gte(nowInstant.minusMinutes(15))))

  def isPlaying(tourId: TourId, userId: UserId): Fu[Boolean] =
    coll.exists(selectTourUser(tourId, userId) ++ selectPlaying)

  def playingUserIds(tourId: TourId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("u", selectTour(tourId) ++ selectPlaying)

  private[tournament] def finishedByPlayerChronological(
      tourId: TourId,
      userId: UserId
  ): Fu[List[Pairing]] =
    coll
      .find(selectTourUser(tourId, userId) ++ selectFinished)
      .sort(chronoSort)
      .cursor[Pairing]()
      .listAll()

  def insert(pairings: List[Pairing]) =
    coll.insert.many {
      pairings.map { p =>
        pairingHandler.write(p) ++ $doc("d" -> nowInstant)
      }
    }.void

  def finishAndGet(g: Game): Fu[Option[Pairing]] =
    if g.aborted then coll.delete.one($id(g.id)).inject(none)
    else
      coll.findAndUpdateSimplified[Pairing](
        selector = $id(g.id),
        update = $set(
          "s" -> g.status.id,
          "w" -> g.winnerColor.map(_.white),
          "t" -> g.ply
        ),
        fetchNewObject = true
      )

  def setBerserk(pairing: Pairing, userId: UserId) = {
    if pairing.user1 == userId then "b1".some
    else if pairing.user2 == userId then "b2".some
    else none
  }.so { field =>
    coll.update
      .one(
        $id(pairing.id),
        $set(field -> true)
      )
      .void
  }

  def sortedCursor(
      tournamentId: TourId,
      userId: Option[UserId],
      batchSize: Int = 0,
      readPref: ReadPref = _.sec
  ): AkkaStreamCursor[Pairing] =
    coll
      .find(selectTour(tournamentId) ++ userId.so(selectUser))
      .sort(recentSort)
      .batchSize(batchSize)
      .cursor[Pairing](readPref)

  private[tournament] def rawStats(tourId: TourId): Fu[List[Bdoc]] =
    coll.aggregateList(maxDocs = 3): framework =>
      import framework.*
      Match(selectTour(tourId)) -> List(
        Project(
          $doc(
            "_id" -> false,
            "w" -> true,
            "t" -> true,
            "b1" -> $doc("$cond" -> $arr("$b1", 1, 0)),
            "b2" -> $doc("$cond" -> $arr("$b2", 1, 0))
          )
        ),
        GroupField("w")(
          "games" -> SumAll,
          "moves" -> SumField("t"),
          "b1" -> SumField("b1"),
          "b2" -> SumField("b2")
        )
      )

  private[tournament] def anonymize(tourId: TourId, userId: UserId)(ghostId: UserId) =
    coll.update.one($doc("tid" -> tourId, "u" -> userId), $set("u.$" -> ghostId)).void
