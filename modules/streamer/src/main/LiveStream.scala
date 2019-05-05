package lila.streamer

import akka.actor._
import akka.pattern.ask
import makeTimeout.short
import scala.concurrent.duration._

import lila.user.User

case class LiveStreams(streams: List[Stream]) {

  def has(streamer: Streamer) = streams.exists(_ is streamer)

  def get(streamer: Streamer) = streams.find(_ is streamer)

  def autoFeatured = LiveStreams {
    streams.filter(_.streamer.approval.autoFeatured)
  }

  def withTitles(lightUser: lila.user.LightUserApi) = LiveStreams.WithTitles(
    this,
    streams.map(_.streamer.userId).flatMap { userId =>
      lightUser.sync(userId).flatMap(_.title) map (userId ->)
    }.toMap
  )
}

object LiveStreams {
  case class WithTitles(live: LiveStreams, titles: Map[User.ID, String]) {
    def titleName(s: Stream) = s"${titles.get(s.streamer.userId).fold("")(_ + " ")}${s.streamer.name}"
  }
}

final class LiveStreamApi(
    asyncCache: lila.memo.AsyncCache.Builder,
    streamingActor: ActorRef
) {

  private val cache = asyncCache.single[LiveStreams](
    name = "streamer.liveStreams",
    f = streamingActor ? Streaming.Get mapTo manifest[LiveStreams] addEffect {
      liveStreams => userIdsCache = liveStreams.streams.map(_.streamer.userId).toSet
    },
    expireAfter = _.ExpireAfterWrite(2 seconds)
  )
  private var userIdsCache = Set.empty[User.ID]

  def all: Fu[LiveStreams] = cache.get
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

  def of(s: Streamer.WithUser): Fu[Streamer.WithUserAndStream] = all.map { live =>
    Streamer.WithUserAndStream(s.streamer, s.user, live get s.streamer)
  }
  def userIds = userIdsCache
  def isStreaming(userId: User.ID) = userIdsCache contains userId
  def one(userId: User.ID): Fu[Option[Stream]] = all.map(_.streams.find(_ is userId))
  def many(userIds: Seq[User.ID]): Fu[List[Stream]] = all.map(_.streams.filter(s => userIds.exists(s.is)))
}
