package lila.round

import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import actorApi.round._
import chess.{ Color, White, Black, Speed }
import lila.chat.Chat
import lila.common.Bus
import lila.game.{ Game, Event }
import lila.hub.{ Trouper, TrouperMap, DuctMap }
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.user.User

final class RoundRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    roundDependencies: RoundRemoteDuct.Dependencies,
    deployPersistence: DeployPersistence,
    scheduleExpiration: Game => Unit,
    chat: akka.actor.ActorSelection,
    messenger: Messenger,
    goneWeightsFor: Game => Fu[(Float, Float)],
    system: akka.actor.ActorSystem
) {

  import RoundRemoteSocket._

  def getGame(gameId: Game.ID): Fu[Option[Game]] = rounds.getOrMake(gameId).getGame addEffect { g =>
    if (!g.isDefined) rounds kill gameId
  }
  def gameIfPresent(gameId: Game.ID): Fu[Option[Game]] = rounds.getIfPresent(gameId).??(_.getGame)
  def updateIfPresent(game: Game): Fu[Game] = rounds.getIfPresent(game.id).fold(fuccess(game))(_.getGame.map(_ | game))

  val rounds = new DuctMap[RoundRemoteDuct](
    mkDuct = id => {
      val duct = new RoundRemoteDuct(
        dependencies = roundDependencies,
        gameId = id,
        isGone = id => ???,
        socketSend = send
      )(new GameProxy(id, deployPersistence.isEnabled, system.scheduler))
      duct.getGame foreach {
        _ foreach { game =>
          scheduleExpiration(game)
          goneWeightsFor(game) map { RoundRemoteDuct.SetGameInfo(game, _) } foreach duct.!
        }
      }
      duct
    },
    accessTimeout = 40 seconds
  )

  def tellRound(gameId: GameId, msg: Any): Unit = rounds.tell(gameId.value, msg)

  private lazy val roundHandler: Handler = {
    case RP.In.ChatSay(roomId, userId, msg) => messenger.watcher(roomId.value, userId, msg)
    case RP.In.TellRoomSri(gameId, P.In.TellSri(sri, user, tpe, o)) => tpe match {
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case Protocol.In.PlayerDo(fullId, tpe, o) => tpe match {
      case "moretime" => tellRound(fullId.gameId, Moretime(fullId.playerId.value))
      case t => logger.warn(s"Unhandled round socket message: $t")
    }
    case ping: Protocol.In.PlayerPing => tellRound(ping.gameId, ping)
    case RP.In.KeepAlives(roomIds) => roomIds foreach { roomId =>
      rounds touchOrMake roomId.value
    }
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("round-out").apply _

  remoteSocketApi.subscribe("round-in", Protocol.In.reader)(
    roundHandler orElse remoteSocketApi.baseHandler
  )
}

object RoundRemoteSocket {

  case class GameId(value: String) extends AnyVal with StringValue {
    def full(playerId: PlayerId) = FullId(s"$value{$playerId.value}")
  }
  case class FullId(value: String) extends AnyVal with StringValue {
    def gameId = GameId(value take Game.gameIdSize)
    def playerId = PlayerId(value drop Game.gameIdSize)
  }
  case class PlayerId(value: String) extends AnyVal with StringValue

  object Protocol {

    object In {

      case class PlayerPing(gameId: GameId, color: Color) extends P.In
      case class PlayerDo(fullId: FullId, tpe: String, msg: JsObject) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "round/w" => PlayerPing(GameId(raw.args), chess.White).some
        case "round/b" => PlayerPing(GameId(raw.args), chess.Black).some
        case "round/do" => raw.get(2) {
          case Array(fullId, payload) => for {
            obj <- Json.parse(payload).asOpt[JsObject]
            tpe <- obj str "t"
          } yield PlayerDo(FullId(fullId), tpe, obj)
        }
        case _ => RP.In.reader(raw)
      }
    }
  }
}
