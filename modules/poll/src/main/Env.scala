package lila.poll

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.Bus
import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    notifyApi: lila.notify.NotifyApi
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  implicit private lazy val coll = db(CollName("user_poll"))

  lazy val api = wire[PollApi]

  /*Bus.subscribeFun("notify") {
    case lila.hub.actorApi.notify.NotifiedBatch(userIds) =>
      api.markAllRead(userIds.map(Notification.Notifies.apply)).unit
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      pov.player.userId ?? { userId =>
        lila.game.Namer.playerText(pov.opponent)(getLightUser) foreach { opponent =>
          api addNotification Notification.make(
            Notification.Notifies(userId),
            CorresAlarm(
              gameId = pov.gameId,
              opponent = opponent
            )
          )
        }
      }
  }*/
}
