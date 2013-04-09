package lila.lobby

import lila.common.PimpedConfig._
import com.typesafe.config.Config
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val settings = new {
    val EntryMax = config getInt "lobby.entry.max"
    val MessageMax = config getInt "lobby.message.max"
    val MessageLifetime = config duration "lobby.message.lifetime"
    val CollectionHook = config getString "lobby.collection.hook"
    val CollectionEntry = config getString "lobby.collection.entry"
    val CollectionMessage = config getString "lobby.collection.message"
  }
  import settings._

  private[lobby] lazy val hookColl = db(CollectionHook)
  private[lobby] lazy val entryColl = db(CollectionEntry)
  private[lobby] lazy val messageColl = db(CollectionMessage)
}

object Env {

  lazy val current = "[boot] lobby" describes new Env(
    config = lila.common.PlayApp loadConfig "lobby",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
