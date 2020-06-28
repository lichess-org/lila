package lila.streamer

import akka.actor._
import akka.pattern.ask
import makeTimeout.short
import scala.concurrent.duration._

import lila.user.User
import lila.memo.CacheApi._

case class LiveStreams(streams: List[Stream]) {

  private lazy val streamerIds: Set[Streamer.Id] = streams.view.map(_.streamer.id).to(Set)

  def has(id: Streamer.Id): Boolean    = streamerIds(id)
  def has(streamer: Streamer): Boolean = has(streamer.id)

  def get(streamer: Streamer) = streams.find(_ is streamer)

  def autoFeatured =
    LiveStreams {
      streams.filter(_.streamer.approval.autoFeatured)
    }

  def withTitles(lightUser: lila.user.LightUserApi) =
    LiveStreams.WithTitles(
      this,
      streams
        .map(_.streamer.userId)
        .flatMap { userId =>
          lightUser.sync(userId).flatMap(_.title) map (userId ->)
        }
        .toMap
    )

  def excludeUsers(userIds: List[User.ID]) =
    copy(
      streams = streams.filterNot(s => userIds contains s.streamer.userId)
    )
}

object LiveStreams {

  case class WithTitles(live: LiveStreams, titles: Map[User.ID, String]) {
    def titleName(s: Stream) = s"${titles.get(s.streamer.userId).fold("")(_ + " ")}${s.streamer.name}"
    def excludeUsers(userIds: List[User.ID]) =
      copy(
        live = live excludeUsers userIds
      )
  }

  implicit val zero = ornicar.scalalib.Zero.instance(WithTitles(LiveStreams(Nil), Map.empty))
}

final class LiveStreamApi(
    cacheApi: lila.memo.CacheApi,
    streamingActor: ActorRef
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val cache = cacheApi.unit[LiveStreams] {
    _.refreshAfterWrite(2 seconds)
      .buildAsyncFuture { _ =>
        streamingActor ? Streaming.Get mapTo manifest[LiveStreams] addEffect { liveStreams =>
          userIdsCache = liveStreams.streams.map(_.streamer.userId).toSet
        }
      }
  }
  private var userIdsCache = Set.empty[User.ID]

  def all: Fu[LiveStreams] = cache.getUnit
  // import org.joda.time.DateTime
  // def all: Fu[LiveStreams] = fuccess(LiveStreams(List(
  //   Stream.Twitch.Stream("thibault", "test stream on lichess.org", Streamer(
  //     _id = Streamer.Id("thibault"),
  //     listed = Streamer.Listed(true),
  //     approval = Streamer.Approval(
  //       requested = false,
  //       granted = true,
  //       ignored = false,
  //       autoFeatured = true,
  //       chatEnabled = true
  //     ),
  //     picturePath = none,
  //     name = Streamer.Name("thibault"),
  //     headline = none,
  //     description = none,
  //     twitch = none,
  //     youTube = none,
  //     seenAt = DateTime.now, // last seen online
  //     liveAt = DateTime.now.some, // last seen streaming
  //     createdAt = DateTime.now,
  //     updatedAt = DateTime.now
  //   ))
  // )))

  def of(s: Streamer.WithUser): Fu[Streamer.WithUserAndStream] =
    all.map { live =>
      Streamer.WithUserAndStream(s.streamer, s.user, live get s.streamer)
    }
  def userIds                                       = userIdsCache
  def isStreaming(userId: User.ID)                  = userIdsCache contains userId
  def one(userId: User.ID): Fu[Option[Stream]]      = all.map(_.streams.find(_ is userId))
  def many(userIds: Seq[User.ID]): Fu[List[Stream]] = all.map(_.streams.filter(s => userIds.exists(s.is)))
}
