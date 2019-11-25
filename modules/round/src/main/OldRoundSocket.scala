package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.{ Color, White, Black, Speed }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.chat.Chat
import lila.common.LightUser
import lila.game.actorApi.{ StartGame, UserStartGame }
import lila.game.{ Game, Event }
import lila.hub.actorApi.Deploy
import lila.hub.actorApi.round.{ IsOnGame, TourStandingOld }
import lila.hub.actorApi.simul.GetHostIds
import lila.hub.Trouper
import lila.socket._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket
import lila.user.User
import makeTimeout.short

object OldRoundSocket {

  case class ChatIds(priv: Chat.Id, pub: Chat.Id) {
    def all = Seq(priv, pub)
  }

  private[round] case class Dependencies(
      system: ActorSystem,
      lightUser: LightUser.Getter,
      sriTtl: FiniteDuration,
      getGame: Game.ID => Fu[Option[Game]]
  )
}
