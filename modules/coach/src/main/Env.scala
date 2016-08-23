package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionCoach = config getString "collection.coach"
  private val CollectionImage = config getString "collection.image"

  private lazy val coachColl = db(CollectionCoach)
  private lazy val imageColl = db(CollectionImage)

  lazy val api = new CoachApi(
    coll = coachColl,
    imageColl = imageColl)

  def cli = new lila.common.Cli {
    def process = {
      case "coach" :: "init" :: username :: Nil => api init username
    }
  }
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    db = lila.db.Env.current)
}
