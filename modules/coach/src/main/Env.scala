package lila.coach

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import akka.actor._
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    getPref: String => Fu[lila.pref.Pref],
    areFriends: (String, String) => Fu[Boolean],
    lightUser: String => Option[lila.common.LightUser],
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle) {

  private val settings = new {
    val CollectionEntry = config getString "collection.entry"
  }
  import settings._

  private val db = new lila.db.Env(config getConfig "mongodb", lifecycle)

  lazy val share = new Share(getPref, areFriends)

  lazy val dataForm = new DataForm

  lazy val jsonView = new JsonView

  private lazy val storage = new Storage(coll = db(CollectionEntry))

  lazy val api = new CoachApi(coll = db(CollectionEntry))

  lazy val aggregator = new Aggregator(
    storage = storage,
    sequencer = system.actorOf(Props(classOf[lila.hub.Sequencer], None)))
}

object Env {

  lazy val current: Env = "coach" boot new Env(
    config = lila.common.PlayApp loadConfig "coach",
    getPref = lila.pref.Env.current.api.getPrefById,
    areFriends = lila.relation.Env.current.api.areFriends,
    lightUser = lila.user.Env.current.lightUser,
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle)
}
