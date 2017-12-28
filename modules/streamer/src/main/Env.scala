package lila.streamer

import scala.concurrent.duration._
import com.typesafe.config.Config
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env
) {

  private val CollectionStreamer = config getString "collection.streamer"
  private val CollectionImage = config getString "collection.image"

  private lazy val streamerColl = db(CollectionStreamer)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "streamer")

  lazy val api = new StreamerApi(
    coll = streamerColl,
    photographer = photographer
  )
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lila.common.PlayApp loadConfig "streamer",
    db = lila.db.Env.current
  )
}
