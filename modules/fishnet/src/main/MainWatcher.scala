package lila.fishnet

import scala.concurrent.duration._

import lila.common.Bus
import lila.hub.actorApi.slack.{ Victory, Warning }
import lila.memo.ExpireSetMemo

// slack alerts for lichess analysis nodes
final private class MainWatcher(
    repo: FishnetRepo
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val alerted = new ExpireSetMemo(12 hour)

  private def isAlerted(client: Client) = alerted get client.key.value

  private def alert(client: Client) =
    if (!isAlerted(client)) {
      alerted put client.key.value
      Bus.publish(Warning(s"Fishnet server ${client.userId} might be down!"), "slack")
    }

  private def unalert(client: Client) =
    if (isAlerted(client)) {
      alerted remove client.key.value
      Bus.publish(Victory(s"Fishnet server ${client.userId} is back!"), "slack")
    }

  private def watch: Funit =
    repo.lichessClients map { clients =>
      clients foreach { client =>
        client.instance foreach { instance =>
          if (!instance.seenRecently) alert(client)
          else unalert(client)
        }
      }
    }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute)(() => watch)
}
