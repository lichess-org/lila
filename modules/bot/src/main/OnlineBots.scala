package lila.bot

import scala.concurrent.duration._

import lila.common.Bus
import lila.hub.actorApi.socket.BotIsOnline
import lila.memo.ExpireCallbackMemo

private final class OnlineBots(
    scheduler: akka.actor.Scheduler
) {

  val cache = new ExpireCallbackMemo(
    10.seconds,
    userId => Bus.publish(BotIsOnline(userId, false), "botIsOnline")
  )

  def setOnline(userId: lila.user.User.ID): Unit = {
    // We must delay the event publication, because caffeine
    // delays the removal listener, therefore when a bot reconnects,
    // the offline event is sent after the online event.
    if (!cache.get(userId)) scheduler.scheduleOnce(1 second) {
      Bus.publish(BotIsOnline(userId, true), "botIsOnline")
    }
    cache.put(userId)
  }
}
