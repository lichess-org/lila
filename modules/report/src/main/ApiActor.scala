package lidraughts.report

import akka.actor._

import lidraughts.hub.actorApi.playban.Playban

private[report] final class ApiActor(
    api: ReportApi
) extends Actor {
  def receive = {
    case Playban(userId, _) => api.maybeAutoPlaybanReport(userId)
  }
}

