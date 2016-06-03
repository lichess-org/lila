package lila.analyse

import chess.Color
import lila.analyse.Analysis
import lila.common.LightUser
import lila.game.{Namer, Game}
import lila.notify.Notification.Notifies
import lila.notify.{Notification, AnalysisFinished, NotifyApi}

final class Notifier(notifyApi: NotifyApi) {

  def notifyAnalysisComplete(analysis: Analysis, game: Game) = {
    val notifies = Notifies(analysis.requestedBy)
    val color = requestByColor(analysis, game)
    val opponent = AnalysisFinished.OpponentName(opponentName(color, game))
    val notifyContent = AnalysisFinished(AnalysisFinished.Id(analysis.id), color, opponent)
    val notification = Notification(notifies, notifyContent)

    notifyApi.addNotification(notification)
  }

  private def requestByColor(analysis: Analysis, game: Game) = {
    if (game.blackPlayer.userId contains analysis.requestedBy) Color.black
    else Color.white
  }

  implicit val lightUser : LightUser.Getter = lila.user.Env.current.lightUser

  private def opponentName(color: Color, game: Game) = {
    val player = game.pov(color).player
    Namer.playerText(player)
  }

}
