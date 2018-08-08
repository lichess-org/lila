package lidraughts.streamer

import akka.actor._
import akka.pattern.ask
import makeTimeout.short
import scala.concurrent.duration._

import lidraughts.user.User

case class LiveStreams(streams: List[Stream]) {

  def has(streamer: Streamer) = streams.exists(_ is streamer)

  def get(streamer: Streamer) = streams.find(_ is streamer)

  def autoFeatured = LiveStreams {
    streams.filter(_.streamer.approval.autoFeatured)
  }

  def withTitles(lightUser: lidraughts.user.LightUserApi) = LiveStreams.WithTitles(
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
    asyncCache: lidraughts.memo.AsyncCache.Builder,
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
  def many(userIds: Seq[User.ID]): Fu[List[Stream]] = all.map(_.streams.filter(s => userIds.exists(s.is)))
}
