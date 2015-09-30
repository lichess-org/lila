package lila.tv

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._

import scala.collection.JavaConversions._
import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: String => Option[lila.common.LightUser],
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    isProd: Boolean) {

  private val FeaturedSelect = config duration "featured.select"
  private val StreamingSearch = config duration "streaming.search"

  lazy val tv = new Tv(tvActor)

  private val tvActor =
    system.actorOf(
      Props(classOf[TvActor], hub.actor.renderer, hub.socket.round, lightUser),
      name = "tv")

  private lazy val streaming = new Streaming(
    system = system,
    renderer = hub.actor.renderer,
    streamerList = streamerList)

  lazy val streamerList = new StreamerList(new {
    import reactivemongo.bson._
    private val coll = db("flag")
    def get = coll.find(BSONDocument("_id" -> "streamer")).one[BSONDocument].map {
      ~_.flatMap(_.getAs[String]("text"))
    }
    def set(text: String) =
      coll.update(BSONDocument("_id" -> "streamer"), BSONDocument("text" -> text), upsert = true).void
  })

  object isStreamer {
    private val cache = lila.memo.MixedCache.single[Set[String]](
      f = streamerList.lichessIds,
      timeToLive = 1 minute,
      default = Set.empty)
    def apply(id: String) = cache get true contains id
  }

  def streamsOnAir = streaming.onAir

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedSelect) {
      tvActor -> TvActor.Select
    }

    scheduler.once(2.seconds) {
      streaming.actor ! Streaming.Search
      scheduler.message(StreamingSearch) {
        streaming.actor -> Streaming.Search
      }
    }
  }
}

object Env {

  lazy val current = "tv" boot new Env(
    config = lila.common.PlayApp loadConfig "tv",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}

