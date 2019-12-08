package lila.message

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._
import lila.user.UserRepo

@Module
private class MessageConfig(
    @ConfigName("collection.thread") val threadColl: CollName,
    @ConfigName("thread.max_per_page") val threadMaxPerPage: MaxPerPage
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    shutup: lila.hub.actors.Shutup,
    notifyApi: lila.notify.NotifyApi,
    relationApi: lila.relation.RelationApi,
    userRepo: UserRepo,
    prefApi: lila.pref.PrefApi,
    spam: lila.security.Spam,
    isOnline: lila.socket.IsOnline,
    lightUser: lila.common.LightUser.GetterSync
) {

  private val config = appConfig.get[MessageConfig]("message")(AutoConfig.loader)

  private lazy val threadColl = db(config.threadColl)

  lazy val repo = wire[ThreadRepo]

  lazy val forms = wire[DataForm]

  lazy val jsonView = wire[JsonView]

  lazy val batch = wire[MessageBatch]

  lazy val api = wire[MessageApi]

  lazy val security = wire[MessageSecurity]

  lila.common.Bus.subscribeFun("gdprErase") {
    case lila.user.User.GDPRErase(user) => api erase user
  }
}
