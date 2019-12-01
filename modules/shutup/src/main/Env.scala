package lila.shutup

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.user.{ User, UserRepo }

@Module
private class ShutupConfig(
    @ConfigName("collection.shutup") val shutupColl: CollName,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    reporter: akka.actor.ActorSelection,
    follows: (User.ID, User.ID) => Fu[Boolean],
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    db: lila.db.Env
)(implicit system: ActorSystem) {

  private val config = appConfig.get[ShutupConfig]("shutup")(AutoConfig.loader)

  private lazy val coll = db(config.shutupColl)

  lazy val api = wire[ShutupApi]

  // api actor
  system.actorOf(Props(new Actor {
    import lila.hub.actorApi.shutup._
    def receive = {
      case RecordPublicForumMessage(userId, text) =>
        api.publicForumMessage(userId, text)
      case RecordTeamForumMessage(userId, text) =>
        api.teamForumMessage(userId, text)
      case RecordPrivateMessage(userId, toUserId, text, major) =>
        api.privateMessage(userId, toUserId, text, major)
      case RecordPrivateChat(chatId, userId, text) =>
        api.privateChat(chatId, userId, text)
      case RecordPublicChat(userId, text, source) =>
        api.publicChat(userId, text, source)
    }
  }), name = config.actorName)
}
