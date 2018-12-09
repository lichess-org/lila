package lila.shutup

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    reporter: akka.actor.ActorSelection,
    follows: (String, String) => Fu[Boolean],
    system: ActorSystem,
    db: lila.db.Env
) {

  private val CollectionShutup = config getString "collection.shutup"
  private val ActorName = config getString "actor.name"

  lazy val api = new ShutupApi(
    coll = coll,
    follows = follows,
    reporter = reporter
  )

  private lazy val coll = db(CollectionShutup)

  // api actor
  system.actorOf(Props(new Actor {
    import lila.hub.actorApi.shutup._
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
    config = lila.common.PlayApp loadConfig "shutup",
    reporter = lila.hub.Env.current.report,
    system = lila.common.PlayApp.system,
    follows = lila.relation.Env.current.api.fetchFollows _,
    db = lila.db.Env.current
  )
}
