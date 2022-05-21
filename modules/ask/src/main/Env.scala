package lila.ask

import com.softwaremill.macwire._
import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    timeline: lila.hub.actors.Timeline
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  implicit private lazy val coll = db(CollName("ask"))

  lazy val api = wire[AskApi]

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
