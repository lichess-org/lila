package lidraughts.draughtsnet

import scala.concurrent.duration._

import lidraughts.hub.actorApi.slack.{ Victory, Warning }
import lidraughts.memo.ExpireSetMemo

private final class MainWatcher(
    repo: DraughtsnetRepo,
    bus: lidraughts.common.Bus,
    scheduler: lidraughts.common.Scheduler
) {

  private val alerted = new ExpireSetMemo(12 hour)

  private def isAlerted(client: Client) = alerted get client.key.value

  private def alert(client: Client) = if (!isAlerted(client)) {
    alerted put client.key.value
    bus.publish(Warning(s"Draughtsnet server ${client.userId} might be down!"), 'slack)
  }

  private def unalert(client: Client) = if (isAlerted(client)) {
    alerted remove client.key.value
    bus.publish(Victory(s"Draughtsnet server ${client.userId} is back!"), 'slack)
  }

  private def watch: Funit = repo.lidraughtsClients map { clients =>
    clients foreach { client =>
      client.instance foreach { instance =>
        if (!instance.seenRecently) alert(client)
        else unalert(client)
      }
    }
  }

  scheduler.future(1 minute, "draughtsnet main watcher")(watch)
}
