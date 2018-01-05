package lila.streamer

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    system: ActorSystem,
    renderer: ActorSelection,
    isOnline: lila.user.User.ID => Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifyApi: lila.notify.NotifyApi,
    hub: lila.hub.Env,
    db: lila.db.Env
) {

  private val CollectionStreamer = config getString "collection.streamer"
  private val CollectionImage = config getString "collection.image"
  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val Keyword = config getString "streaming.keyword"
  private val GoogleApiKey = config getString "streaming.google.api_key"
  private val TwitchClientId = config getString "streaming.twitch.client_id"

  private lazy val streamerColl = db(CollectionStreamer)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "streamer")

  lazy val api = new StreamerApi(
    coll = streamerColl,
    asyncCache = asyncCache,
    photographer = photographer,
    notifyApi = notifyApi
  )

  lazy val pager = new StreamerPager(
    coll = streamerColl,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  private val streamingActor = system.actorOf(Props(new Streaming(
    renderer = renderer,
    api = api,
    isOnline = isOnline,
    timeline = hub.actor.timeline,
    keyword = Stream.Keyword(Keyword),
    googleApiKey = GoogleApiKey,
    twitchClientId = TwitchClientId
  )))

  object liveStreams {
    import lila.user.User
    import makeTimeout.short
    import akka.pattern.ask
    private val cache = asyncCache.single[Stream.LiveStreams](
      name = "streamer.liveStreams",
      f = streamingActor ? Streaming.Get mapTo manifest[Stream.LiveStreams] addEffect {
        liveStreams => userIdsCache = liveStreams.streams.map(_.streamer.userId).toSet
      },
      expireAfter = _.ExpireAfterWrite(2 seconds)
    )
    private var userIdsCache = Set.empty[User.ID]

    def all: Fu[Stream.LiveStreams] = cache.get
    def of(s: Streamer.WithUser): Fu[Streamer.WithUserAndStream] = all.map { live =>
      Streamer.WithUserAndStream(s.streamer, s.user, live get s.streamer)
    }
    def userIds = userIdsCache
    def isStreaming(userId: User.ID) = userIdsCache contains userId
  }

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.user.User.Active(user) if !user.seenRecently => api.setSeenAt(user)
      }
    })), 'userActive
  )
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lila.common.PlayApp loadConfig "streamer",
    system = lila.common.PlayApp.system,
    renderer = lila.hub.Env.current.actor.renderer,
    isOnline = lila.user.Env.current.isOnline,
    asyncCache = lila.memo.Env.current.asyncCache,
    notifyApi = lila.notify.Env.current.api,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current
  )
}
