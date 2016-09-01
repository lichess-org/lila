package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    notifyApi: lila.notify.NotifyApi,
    db: lila.db.Env) {

  private val CollectionCoach = config getString "collection.coach"
  private val CollectionReview = config getString "collection.review"
  private val CollectionImage = config getString "collection.image"

  private lazy val coachColl = db(CollectionCoach)
  private lazy val reviewColl = db(CollectionReview)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new Photographer(imageColl)

  lazy val api = new CoachApi(
    coachColl = coachColl,
    reviewColl = reviewColl,
    photographer = photographer,
    notifyApi = notifyApi)

  lazy val pager = new CoachPager(api)

  def cli = new lila.common.Cli {
    def process = {
      case "coach" :: "enable" :: username :: Nil  => api.toggleByMod(username, true)
      case "coach" :: "disable" :: username :: Nil => api.toggleByMod(username, false)
    }
  }
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    notifyApi = lila.notify.Env.current.api,
    db = lila.db.Env.current)
}
