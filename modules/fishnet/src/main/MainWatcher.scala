package lila.fishnet

import scala.concurrent.duration._

import lila.hub.actorApi.slack.{ Victory, Warning }
import lila.memo.ExpireSetMemo

private final class MainWatcher(
    repo: FishnetRepo,
    bus: lila.common.Bus,
    scheduler: lila.common.Scheduler
) {

  private val alerted = new ExpireSetMemo(12 hour)

  private def isAlerted(client: Client) = alerted get client.key.value

  private def alert(client: Client) = if (!isAlerted(client)) {
    alerted put client.key.value
    bus.publish(Warning(s"Fishnet server ${client.userId} might be down!"), 'slack)
  }

  private def unalert(client: Client) = if (isAlerted(client)) {
    alerted remove client.key.value
    bus.publish(Victory(s"Fishnet server ${client.userId} is back!"), 'slack)
  }

  private def watch: Funit = repo.lichessClients map { clients =>
    clients foreach { client =>
      client.instance foreach { instance =>
        if (!instance.seenRecently) alert(client)
        else unalert(client)
      }
    }
  }

  scheduler.future(1 minute, "fishnet main watcher")(watch)
}
