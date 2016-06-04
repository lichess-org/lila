package lila.analyse

import akka.actor.ActorSelection
import chess.Color
import akka.pattern.ask
import lila.common.LightUser
import lila.game.{Namer, Game}
import lila.hub.actorApi.HasUserId
import lila.hub.actorApi.map.Ask
import lila.notify.Notification.Notifies
import lila.notify.{Notification, AnalysisFinished, NotifyApi}

import makeTimeout.short

final class Notifier(notifyApi: NotifyApi) {

  def notifyAnalysisComplete(analysis: Analysis, game: Game, roundSocket: ActorSelection) = {
    // Only notify for player requests - not internal server ones
    analysis.uid.foreach {
      requesterId =>
        // If the user is not on the analysis page, inform them that their game analysis is complete
        isUserPresent(analysis.id, roundSocket, analysis.requestedBy) map { isPresent =>
          if (!isPresent) {
            val notifies = Notifies(analysis.requestedBy)
            val color = requestByColor(analysis, game)
            val opponent = AnalysisFinished.OpponentName(opponentName(color, game))
            val notifyContent = AnalysisFinished(AnalysisFinished.Id(analysis.id), color, opponent)
            val notification = Notification(notifies, notifyContent)

            notifyApi.addNotification(notification)
          }
        }
    }
  }

  /**
    * Checks whether the user is already on the analysis page
    */
  private def isUserPresent(id: String, roundSocket: ActorSelection, userId: String) : Fu[Boolean] =
    roundSocket ? Ask(id, HasUserId(userId)) mapTo manifest[Boolean]

  private def requestByColor(analysis: Analysis, game: Game) = {
    if (game.blackPlayer.userId contains analysis.requestedBy) Color.black
    else Color.white
  }

  implicit val lightUser : LightUser.Getter = lila.user.Env.current.lightUser

  private def opponentName(color: Color, game: Game) = {
    val player = game opponent color
    Namer.playerText(player)
  }

}
