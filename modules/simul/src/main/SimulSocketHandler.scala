package lila.simul

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.chat.Chat
import lila.common.ApiVersion
import lila.hub.actorApi.map._
import lila.hub.Trouper
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.{ Sri, SocketVersion }
import lila.socket.{ RemoteSocket, Handler }
import lila.user.{ User, UserRepo }

private[simul] final class SimulSocketHandler(
    remoteSocket: RemoteSocket,
    hub: lila.hub.Env,
    socketMap: SocketMap,
    chat: ActorSelection,
    exists: Simul.ID => Fu[Boolean]
) {

  private object In {
    val Connect = "connect"
  }
  private object Out {
  }

  val remoteHandler: RemoteSocket.Handler = {
    case (In.Connect, args) => args.split(" ", 4) match {
      case Array(simulId, sri, userId, version) => parseIntOption(version) foreach { v =>
        UserRepo enabledById userId foreach { user =>
          val socket = socketMap getOrMake simulId
          socket.ask[Connected](Join(Sri(sri), user, SocketVersion(v).some, _)) map {
            case Connected(member) => println(member, "connected!")
          }
        }
      }
      case _ =>
    }
    case ("talk", _) =>
  }

  remoteSocket.subscribe("simul-in")(
    remoteHandler orElse remoteSocket.defaultHandler
  )

  def join(
    simulId: String,
    sri: Sri,
    user: Option[User],
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[Option[JsSocketHandler]] = ???
  // exists(simulId) flatMap {
  //   _ ?? {
  //     val socket = socketMap getOrMake simulId
  //     socket.ask[Connected](Join(sri, user, version, _)) map {
  //       case Connected(member) => Handler.iteratee(
  //         hub,
  //         lila.chat.Socket.in(
  //           chatId = Chat.Id(simulId),
  //           member = member,
  //           chat = chat,
  //           publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simulId).some
  //         ),
  //         member,
  //         socket,
  //         sri,
  //         apiVersion
  //       ) -> enum
  //     } map some
  //   }
  // }
}
