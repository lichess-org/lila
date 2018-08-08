package lidraughts.shutup

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    reporter: akka.actor.ActorSelection,
    follows: (String, String) => Fu[Boolean],
    system: ActorSystem,
    db: lidraughts.db.Env
) {

  private val settings = new {
    val CollectionShutup = config getString "collection.shutup"
    val ActorName = config getString "actor.name"
  }
  import settings._

  lazy val api = new ShutupApi(
    coll = coll,
    follows = follows,
    reporter = reporter
  )

  private lazy val coll = db(CollectionShutup)

  // api actor
  system.actorOf(Props(new Actor {
    import lidraughts.hub.actorApi.shutup._
    def receive = {
      case RecordPublicForumMessage(userId, text) =>
        api.publicForumMessage(userId, text)
      case RecordTeamForumMessage(userId, text) =>
        api.teamForumMessage(userId, text)
      case RecordPrivateMessage(userId, toUserId, text) =>
        api.privateMessage(userId, toUserId, text)
      case RecordPrivateChat(chatId, userId, text) =>
        api.privateChat(chatId, userId, text)
      case RecordPublicChat(userId, text, source) =>
        api.publicChat(userId, text, source)
    }
  }), name = ActorName)
}

object Env {

  lazy val current: Env = "shutup" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "shutup",
    reporter = lidraughts.hub.Env.current.actor.report,
    system = lidraughts.common.PlayApp.system,
    follows = lidraughts.relation.Env.current.api.fetchFollows _,
    db = lidraughts.db.Env.current
  )
}
