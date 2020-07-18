package lila.shutup

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.user.UserRepo

@Module
private class ShutupConfig(
    @ConfigName("collection.shutup") val shutupColl: CollName,
    @ConfigName("actor.name") val actorName: String
)

final class Env(
    appConfig: Configuration,
    reporter: lila.hub.actors.Report,
    relationApi: lila.relation.RelationApi,
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[ShutupConfig]("shutup")(AutoConfig.loader)

  private lazy val coll = db(config.shutupColl)

  lazy val api = wire[ShutupApi]

  // api actor
  system.actorOf(
    Props(new Actor {
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
    }),
    name = config.actorName
  )
}
