package lila.streamer

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    system: ActorSystem,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private val CollectionStreamer = config getString "collection.streamer"
  private val CollectionImage = config getString "collection.image"
  private val MaxPerPage = config getInt "paginator.max_per_page"

  private lazy val streamerColl = db(CollectionStreamer)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "streamer")

  lazy val api = new StreamerApi(
    coll = streamerColl,
    asyncCache = asyncCache,
    photographer = photographer
  )

  lazy val pager = new StreamerPager(
    coll = streamerColl,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  private lazy val importer = new Importer(api, db("flag"))

  def cli = new lila.common.Cli {
    def process = {
      case "streamer" :: "import" :: Nil => importer.apply inject "done"
    }
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
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current
  )
}
