package lila.fishnet

import scala.concurrent.duration._

import lila.common.Bus
import lila.memo.ExpireSetMemo

final private class MainWatcher(
    repo: FishnetRepo
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val alerted = new ExpireSetMemo(12 hour)

  private def isAlerted(client: Client) = alerted get client.key.value

  private def alert(client: Client) =
    if (!isAlerted(client)) {
      alerted put client.key.value
      Bus.publish(lila.hub.actorApi.mod.Alert(s"Shoginet server ${client.userId} might be down!"), "alert")
    }

  private def unalert(client: Client) =
    if (isAlerted(client)) {
      alerted remove client.key.value
      Bus.publish(lila.hub.actorApi.mod.Alert(s"Shoginet server ${client.userId} is back!"), "alert")
    }

  private def watch: Funit =
    repo.lishogiClients map { clients =>
      clients foreach { client =>
        client.instance foreach { instance =>
          if (!instance.seenRecently) alert(client)
          else unalert(client)
        }
      }
    }

  system.scheduler.scheduleWithFixedDelay(1 minute, 2 minute)(() => watch.unit)
}
