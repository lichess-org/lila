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
    photographer: Photographer
) {

  import BsonHandlers._

  def byId(id: Streamer.Id): Fu[Option[Streamer]] = coll.byId[Streamer](id.value)

  def find(username: String): Fu[Option[Streamer.WithUser]] =
    UserRepo named username flatMap { _ ?? find }

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    byId(Streamer.Id(user.id)) map2 withUser(user)

  def findOrInit(user: User): Fu[Option[Streamer.WithUser]] =
    find(user) orElse {
      val s = Streamer.WithUser(Streamer make user, user)
      coll insert s.streamer inject s.some
    }

  def save(s: Streamer): Funit =
    coll.update($id(s.id), s, upsert = true).void

  def setSeenAt(user: User): Funit =
    listedIdsCache.get flatMap { ids =>
      ids.contains(Streamer.Id(user.id)) ??
        coll.update($id(user.id), $set("sorting.seenAt" -> DateTime.now)).void
    }

  def update(s: Streamer, data: StreamerForm.UserData): Funit =
    coll.update($id(s.id), data(s)).void

  def create(u: User): Funit =
    isStreamer(u) flatMap { exists =>
      !exists ?? coll.insert(Streamer make u).void
    }

  def isStreamer(user: User): Fu[Boolean] = listedIdsCache.get.map(_ contains Streamer.Id(user.id))

  def uploadPicture(s: Streamer, picture: Photographer.Uploaded): Funit =
    photographer(s.id.value, picture).flatMap { pic =>
      coll.update($id(s.id), $set("picturePath" -> pic.path)).void
    }

  def deletePicture(s: Streamer): Funit =
    coll.update($id(s.id), $unset("picturePath")).void

  private def withUser(user: User)(streamer: Streamer) = Streamer.WithUser(streamer, user)

  private def selectListedApproved = $doc(
    "listed" -> true,
    "approved" -> true
  )

  private val listedIdsCache = asyncCache.single[Set[Streamer.Id]](
    name = "streamer.ids",
    f = coll.distinctWithReadPreference[Streamer.Id, Set]("_id", selectListedApproved.some, ReadPreference.secondaryPreferred),
    expireAfter = _.ExpireAfterWrite(1 hour)
  )
}
