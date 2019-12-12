package lila.streamer

import org.joda.time.DateTime
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.user.{ User, UserRepo }

final class StreamerApi(
    coll: Coll,
    asyncCache: lila.memo.AsyncCache.Builder,
    photographer: Photographer,
    notifyApi: lila.notify.NotifyApi
) {

  import BsonHandlers._

  def withColl[A](f: Coll => A): A = f(coll)

  def byId(id: Streamer.Id): Fu[Option[Streamer]] = coll.byId[Streamer](id.value)
  def byIds(ids: Iterable[Streamer.Id]): Fu[List[Streamer]] = coll.byIds[Streamer](ids.map(_.value))

  def find(username: String): Fu[Option[Streamer.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    byId(Streamer.Id(user.id)) map2 withUser(user)

  def findOrInit(user: User): Fu[Option[Streamer.WithUser]] =
    find(user) orElse {
      val s = Streamer.WithUser(Streamer make user, user)
      coll insert s.streamer inject s.some
    }

  def withUser(s: Stream): Fu[Option[Streamer.WithUserAndStream]] =
    UserRepo named s.streamer.userId map {
      _ map { user => Streamer.WithUserAndStream(s.streamer, user, s.some) }
    }

  def withUsers(live: LiveStreams): Fu[List[Streamer.WithUserAndStream]] =
    live.streams.map(withUser).sequenceFu.map(_.flatten)

  def allListedIds: Fu[Set[Streamer.Id]] = listedIdsCache.get

  def setSeenAt(user: User): Funit =
    listedIdsCache.get flatMap { ids =>
      ids.contains(Streamer.Id(user.id)) ??
        coll.update($id(user.id), $set("seenAt" -> DateTime.now)).void
    }

  def setLiveNow(ids: List[Streamer.Id]): Funit =
    coll.update($doc("_id" $in ids), $set("liveAt" -> DateTime.now), multi = true).void

  private[streamer] def mostRecentlySeenIds(ids: List[Streamer.Id], max: Int): Fu[Set[Streamer.Id]] =
    coll.find($inIds(ids))
      .sort($doc("seenAt" -> -1))
      .list[Bdoc](max) map {
        _ flatMap {
          _.getAs[Streamer.Id]("_id")
        }
      } map (_.toSet)

  def update(prev: Streamer, data: StreamerForm.UserData, asMod: Boolean): Fu[Streamer.ModChange] = {
    val streamer = data(prev, asMod)
    coll.update($id(streamer.id), streamer) >>-
      listedIdsCache.refresh inject {
        val modChange = Streamer.ModChange(
          list = prev.approval.granted != streamer.approval.granted option streamer.approval.granted,
          feature = prev.approval.autoFeatured != streamer.approval.autoFeatured option streamer.approval.autoFeatured
        )
        import lila.notify.Notification.Notifies
        import lila.notify.{ Notification, NotifyApi }
        ~modChange.list ??
          notifyApi.addNotification(Notification.make(
            Notifies(streamer.userId),
            lila.notify.GenericLink(
              url = s"/streamer/edit",
              title = "Listed on /streamer".some,
              text = "Your streamer page is public".some,
              icon = ""
            )
          ))
        modChange
      }
  }

  def demote(userId: User.ID): Funit =
    coll.update(
      $id(userId),
      $set(
        "approval.requested" -> false,
        "approval.granted" -> false,
        "approval.autoFeatured" -> false
      )
    ).void

  def create(u: User): Funit =
    isStreamer(u) flatMap { exists =>
      !exists ?? coll.insert(Streamer make u).void
    }

  def isStreamer(user: User): Fu[Boolean] = listedIdsCache.get.dmap(_ contains Streamer.Id(user.id))

  def uploadPicture(s: Streamer, picture: Photographer.Uploaded): Funit =
    photographer(s.id.value, picture).flatMap { pic =>
      coll.update($id(s.id), $set("picturePath" -> pic.path)).void
    }

  def deletePicture(s: Streamer): Funit =
    coll.update($id(s.id), $unset("picturePath")).void

  // unapprove after a week if you never streamed
  def autoDemoteFakes: Funit =
    coll.update(
      $doc(
        "liveAt" $exists false,
        "approval.granted" -> true,
        "approval.lastGrantedAt" $lt DateTime.now.minusWeeks(1)
      ),
      $set(
        "approval.granted" -> false,
        "approval.autoFeatured" -> false,
        "demoted" -> true
      ),
      multi = true
    ).void

  object approval {

    def request(user: User) = find(user) flatMap {
      _.filter(!_.streamer.approval.granted) ?? { s =>
        coll.updateField($id(s.streamer.id), "approval.requested", true).void
      }
    }

    def countRequests: Fu[Int] = coll.countSel($doc(
      "approval.requested" -> true,
      "approval.ignored" -> false
    ))
  }

  private def withUser(user: User)(streamer: Streamer) = Streamer.WithUser(streamer, user)

  private def selectListedApproved = $doc(
    "listed" -> true,
    "approval.granted" -> true
  )

  private val listedIdsCache = asyncCache.single[Set[Streamer.Id]](
    name = "streamer.ids",
    f = coll.distinctWithReadPreference[Streamer.Id, Set](
      "_id",
      selectListedApproved.some,
      ReadPreference.secondaryPreferred
    ),
    expireAfter = _.ExpireAfterWrite(1 hour),
    resultTimeout = 10.seconds,
    monitor = false
  )
}
