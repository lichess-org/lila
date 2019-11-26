package lila.bot

import scala.concurrent.duration._

import lila.hub.actorApi.socket.BotIsOnline
import lila.memo.ExpireCallbackMemo

private final class OnlineBots(
    bus: lila.common.Bus,
    scheduler: akka.actor.Scheduler
) {

  val cache = new ExpireCallbackMemo(
    10.seconds,
    userId => bus.publish(BotIsOnline(userId, false), 'botIsOnline)
  )

  def setOnline(userId: lila.user.User.ID): Unit = {
    // We must delay the event publication, because caffeine
    // delays the removal listener, therefore when a bot reconnects,
    // the offline event is sent after the online event.
    if (!cache.get(userId)) scheduler.scheduleOnce(1 second) {
      bus.publish(BotIsOnline(userId, true), 'botIsOnline)
    }
    cache.put(userId)
  }
}
