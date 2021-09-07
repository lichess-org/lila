package lila.streamer

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.db.Photographer
import lila.memo.CacheApi._
import lila.memo.PicfitApi
import lila.user.{ User, UserRepo }

final class StreamerApi(
    coll: Coll,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    picfitApi: PicfitApi,
    notifyApi: lila.notify.NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def withColl[A](f: Coll => A): A = f(coll)

  def byId(id: Streamer.Id): Fu[Option[Streamer]]           = coll.byId[Streamer](id.value)
  def byIds(ids: Iterable[Streamer.Id]): Fu[List[Streamer]] = coll.byIds[Streamer](ids.map(_.value))

  def find(username: String): Fu[Option[Streamer.WithUser]] =
    userRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    byId(Streamer.Id(user.id)) dmap {
      _ map { Streamer.WithUser(_, user) }
    }

  def findOrInit(user: User): Fu[Option[Streamer.WithUser]] =
    find(user) orElse {
      val s = Streamer.WithUser(Streamer make user, user)
      coll.insert.one(s.streamer) inject s.some
    }

  def withUser(s: Stream): Fu[Option[Streamer.WithUserAndStream]] =
    userRepo named s.streamer.userId dmap {
      _ map { user =>
        Streamer.WithUserAndStream(s.streamer, user, s.some)
      }
    }

  def withUsers(live: LiveStreams): Fu[List[Streamer.WithUserAndStream]] =
    live.streams.map(withUser).sequenceFu.dmap(_.flatten)

  def allListedIds: Fu[Set[Streamer.Id]] = cache.listedIds.getUnit

  def setSeenAt(user: User): Funit =
    cache.listedIds.getUnit flatMap { ids =>
      ids.contains(Streamer.Id(user.id)) ??
        coll.update.one($id(user.id), $set("seenAt" -> DateTime.now)).void
    }

  def setLiveNow(ids: List[Streamer.Id]): Funit =
    coll.update.one($doc("_id" $in ids), $set("liveAt" -> DateTime.now), multi = true) >>
      cache.candidateIds.getUnit.map { candidateIds =>
        if (ids.exists(candidateIds.contains)) cache.candidateIds.invalidateUnit()
      }

  def update(prev: Streamer, data: StreamerForm.UserData, asMod: Boolean): Fu[Streamer.ModChange] = {
    val streamer = data(prev, asMod)
    coll.update.one($id(streamer.id), streamer) >>-
      cache.listedIds.invalidateUnit() inject {
        val modChange = Streamer.ModChange(
          list = prev.approval.granted != streamer.approval.granted option streamer.approval.granted,
          tier = prev.approval.tier != streamer.approval.tier option streamer.approval.tier,
          decline = !streamer.approval.granted && !streamer.approval.requested && prev.approval.requested
        )
        import lila.notify.Notification.Notifies
        import lila.notify.Notification
        ~modChange.list ?? {
          notifyApi.addNotification(
            Notification.make(
              Notifies(streamer.userId),
              lila.notify.GenericLink(
                url = "/streamer/edit",
                title = "Listed on /streamer".some,
                text = "Your streamer page is public".some,
                icon = ""
              )
            )
          ) >>- cache.candidateIds.invalidateUnit()
        }
        modChange
      }
  }

  def demote(userId: User.ID): Funit =
    coll.update
      .one(
        $id(userId),
        $set(
          "approval.requested" -> false,
          "approval.granted"   -> false
        )
      )
      .void

  def delete(user: User): Funit =
    coll.delete.one($id(user.id)).void

  def create(u: User): Funit =
    coll.insert.one(Streamer make u).void.recover(lila.db.ignoreDuplicateKey)

  def isPotentialStreamer(user: User): Fu[Boolean] =
    cache.listedIds.getUnit.dmap(_ contains Streamer.Id(user.id))

  def isCandidateStreamer(user: User): Fu[Boolean] =
    cache.candidateIds.getUnit.dmap(_ contains Streamer.Id(user.id))

  def isActualStreamer(user: User): Fu[Boolean] =
    isPotentialStreamer(user) >>& !isCandidateStreamer(user)

  def uploadPicture(s: Streamer, picture: PicfitApi.Uploaded, by: User): Funit = {
    picfitApi
      .upload("streamer", picture, userId = by.id) flatMap { pic =>
      coll.update.one($id(s.id), $set("picture" -> pic.id)).void
    }
  }.logFailure(logger branch "upload")

  // unapprove after a week if you never streamed
  def autoDemoteFakes: Funit =
    coll.update
      .one(
        $doc(
          "liveAt" $exists false,
          "approval.granted" -> true,
          "approval.lastGrantedAt" $lt DateTime.now.minusWeeks(1)
        ),
        $set(
          "approval.granted" -> false,
          "demoted"          -> true
        ),
        multi = true
      )
      .void

  object approval {

    def request(user: User) =
      find(user) flatMap {
        _.filter(!_.streamer.approval.granted) ?? { s =>
          coll.updateField($id(s.streamer.id), "approval.requested", true).void
        }
      }

    def countRequests: Fu[Int] =
      coll.countSel(
        $doc(
          "approval.requested" -> true,
          "approval.ignored"   -> false
        )
      )
  }

  def sameChannels(streamer: Streamer): Fu[List[Streamer]] =
    coll
      .find(
        $doc(
          "$or" -> List(
            streamer.twitch.map(_.userId).map { t =>
              $doc("twitch.userId" -> t)
            },
            streamer.youTube.map(_.channelId).map { t =>
              $doc("youTube.channelId" -> t)
            }
          ).flatten,
          "_id" $ne streamer.userId
        )
      )
      .sort($sort desc "createdAt")
      .cursor[Streamer](readPreference = ReadPreference.secondaryPreferred)
      .list(10)

  private object cache {

    private def selectListedApproved =
      $doc(
        "listed"           -> true,
        "approval.granted" -> true
      )

    val listedIds = cacheApi.unit[Set[Streamer.Id]] {
      _.refreshAfterWrite(1 hour)
        .buildAsyncFuture { _ =>
          coll.secondaryPreferred.distinctEasy[Streamer.Id, Set](
            "_id",
            selectListedApproved
          )
        }
    }

    val candidateIds = cacheApi.unit[Set[Streamer.Id]] {
      _.refreshAfterWrite(1 hour)
        .buildAsyncFuture { _ =>
          coll.secondaryPreferred.distinctEasy[Streamer.Id, Set](
            "_id",
            selectListedApproved ++ $doc("liveAt" $exists false)
          )
        }
    }
  }
}
