package lila.streamer

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import makeTimeout.short

import lila.user.User

case class LiveStreams(streams: List[Stream]) {

  def has(streamer: Streamer) = streams.exists(_ is streamer)

  def get(streamer: Streamer) = streams.find(_ is streamer)

  def autoFeatured = LiveStreams {
    streams.filter(_.streamer.approval.autoFeatured)
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
  def of(s: Streamer.WithUser): Fu[Streamer.WithUserAndStream] = all.map { live =>
    Streamer.WithUserAndStream(s.streamer, s.user, live get s.streamer)
  }
  def userIds = userIdsCache
  def isStreaming(userId: User.ID) = userIdsCache contains userId
  def one(userId: User.ID): Fu[Option[Stream]] = all.map(_.streams.find(_ is userId))
}
