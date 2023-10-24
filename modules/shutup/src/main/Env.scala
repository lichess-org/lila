package lila.shutup

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.Configuration

import lila.common.config.*
import lila.user.UserRepo

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
)(using ec: Executor, system: ActorSystem):

  private val config = appConfig.get[ShutupConfig]("shutup")(AutoConfig.loader)

  private lazy val coll = db(config.shutupColl)

  lazy val api = wire[ShutupApi]

  // api actor
  system.actorOf(
    Props(new Actor:
      import lila.hub.actorApi.shutup.*
      def receive =
        case RecordTeamForumMessage(userId, text)         => api.teamForumMessage(userId, text)
        case RecordPrivateMessage(userId, toUserId, text) => api.privateMessage(userId, toUserId, text)
        case RecordPrivateChat(chatId, userId, text)      => api.privateChat(chatId, userId, text)
        case RecordPublicText(userId, text, source)       => api.publicText(userId, text, source)
    ),
    name = config.actorName
  )
