package lila.pref

import akka.actor._
import com.softwaremill.macwire.Module
import scala.concurrent.duration._

import lila.common.config.CollName

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext, system: ActorSystem) {

  val api = new PrefApi(db(CollName("pref")), cacheApi)

  system.scheduler.scheduleWithFixedDelay(10 minute, 10 minute) { () =>
    api.corresEmailNotifUsers foreach { lila.mon.pref.emailNotifNbUsers.update(_) }
  }
}
