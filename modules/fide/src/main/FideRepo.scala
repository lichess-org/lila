package lila.fide

import scala.util.Success
import java.time.YearMonth
import chess.{ FideId, FideTC }
import chess.rating.Elo
import reactivemongo.api.bson.*

import lila.core.fide.{ Federation, FidePlayerOrder }
import lila.db.dsl.{ *, given }

final private class FideRepo(
    private[fide] val playerColl: Coll,
    private[fide] val ratingColl: Coll,
    private[fide] val federationColl: Coll,
    private[fide] val followerColl: Coll
)(using Executor):

  object player:
    given BSONDocumentHandler[FidePlayer.PlayerPhoto] = Macros.handler
    given handler: BSONDocumentHandler[FidePlayer] = Macros.handler
    val selectActive: Bdoc = $doc("inactive".$ne(true))
    def selectFed(fed: Federation.Id): Bdoc = $doc("fed" -> fed)
    def sortStandard: Bdoc = $sort.desc("standard")
    def sortBy(o: FidePlayerOrder) = o match
      case FidePlayerOrder.name => $sort.asc("name")
      case FidePlayerOrder.standard => $sort.desc("standard")
      case FidePlayerOrder.rapid => $sort.desc("rapid")
      case FidePlayerOrder.blitz => $sort.desc("blitz")
      case FidePlayerOrder.year => $sort.desc("year")
      case FidePlayerOrder.follow => $empty // TODO
    def fetch(id: FideId): Fu[Option[FidePlayer]] = playerColl.byId[FidePlayer](id)
    def fetch(ids: Seq[FideId]): Fu[List[FidePlayer]] =
      playerColl.find($inIds(ids)).cursor[FidePlayer](ReadPref.sec).listAll()
    def countAll = playerColl.count()
    def setPhoto(id: FideId, photo: FidePlayer.PlayerPhoto): Funit =
      playerColl.updateField($id(id), "photo", photo).void
    def setPhotoCredit(p: FidePlayer, credit: Option[String]): Funit =
      playerColl.updateOrUnsetField($id(p.id) ++ $doc("photo.id".$exists(true)), "photo.credit", credit).void
    def setDeceasedYear(id: FideId, year: Option[Int]): Funit =
      playerColl.updateOrUnsetField($id(id), "deceasedYear", year).void

  object rating:
    private given BSONHandler[YearMonth] =
      quickHandler({ case BSONString(s) => YearMonth.parse(s) }, ym => BSONString(ym.toString))
    given BSONHandler[FideRatingHistory.RatingPoint] = tupleHandler
    given BSONDocumentHandler[FideRatingHistory] = Macros.handler
    def get(id: FideId): Fu[FideRatingHistory] =
      ratingColl.byId[FideRatingHistory](id).map(_ | FideRatingHistory.empty(id))
    def set(id: FideId, date: YearMonth, elos: Map[FideTC, Elo]): Funit = elos.nonEmpty.so:
      for
        history <- get(id)
        updated = history.set(date, elos)
        _ <- ratingColl.update.one($id(id), updated, upsert = true)
      yield ()

  object federation:
    given BSONDocumentHandler[Federation.Stats] = Macros.handler
    given handler: BSONDocumentHandler[Federation] = Macros.handler
    def upsert(fed: Federation): Funit =
      federationColl.update.one($id(fed.id), fed, upsert = true).void
    def fetch(code: Federation.Id): Fu[Option[Federation]] = federationColl.byId[Federation](code)

  object follower:
    // { _id: '14204118/thibault', u: 'thibault', p: 14204118 }
    type FollowId = String
    private object followId:
      def make(p: FideId, u: UserId) = s"$p/$u"
      def toUserId(id: FollowId): UserId = UserId(id.drop(id.indexOf('/') + 1))
      def toFideId(id: FollowId): FideId = FideId(id.takeWhile(_ != '/').toInt)

    def followers(p: FideId): Fu[Set[UserId]] = (p.value > 0).so:
      for ids <- followerColl.distinctEasy[FollowId, Set]("_id", "_id".$startsWith(s"$p/"))
      yield ids.map(followId.toUserId)
    def follow(u: UserId, p: FideId) = playerColl
      .exists($id(p))
      .flatMapz:
        followerColl.update.one($id(followId.make(p, u)), $doc("u" -> u, "p" -> p), upsert = true).void
    def unfollow(u: UserId, p: FideId) = followerColl.delete.one($id(followId.make(p, u))).void
    def isFollowing(u: UserId, p: FideId) = followerColl.exists($id(followId.make(p, u)))

    def count(u: UserId): Fu[Int] = followerColl.countSel($doc("u" -> u))

    def withFollows(players: Seq[FidePlayer], u: UserId): Fu[Seq[FidePlayer.WithFollow]] =
      val ids = players.map(_.id).map(followId.make(_, u))
      followerColl
        .distinctEasy[FollowId, Set]("_id", "_id".$in(ids))
        .map(_.map(followId.toFideId))
        .map: followedIds =>
          players.map: p =>
            FidePlayer.WithFollow(p, followedIds.contains(p.id))
