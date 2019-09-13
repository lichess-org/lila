package lila.lobby

import play.api.libs.json._

import actorApi._
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.Sri
import lila.user.{ User, UserRepo }

final class LobbyRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    lobby: LobbyTrouper,
    socket: LobbySocket,
    blocking: User.ID => Fu[Set[User.ID]],
    controller: LobbySocketMember => lila.socket.Handler.Controller,
    bus: lila.common.Bus
) {

  import LobbyRemoteSocket.Protocol._

  private val handler: Handler = {
    case P.In.ConnectSri(sri, userOpt) =>
      userOpt map P.In.ConnectUser.apply foreach remoteSocketApi.baseHandler.lift
      userOpt ?? UserRepo.enabledById foreach { user =>
        (user ?? (u => blocking(u.id))) foreach { blocks =>
          val member = actorApi.LobbySocketMember(js => send(P.Out.tellSri(sri, js)), user, blocks, sri)
          socket ! actorApi.JoinRemote(member)
        }
      }
    case P.In.DisconnectSri(sri) => socket ! actorApi.LeaveRemote(sri)

    case P.In.DisconnectAll =>
      lobby ! actorApi.LeaveAllRemote
      socket ! actorApi.LeaveAllRemote

    case tell @ P.In.TellSri(sri, user, typ, msg) if messagesHandled(typ) =>
      lila.mon.socket.remote.lobby.tellSri(typ)
      socket.ask[Option[LobbyRemoteSocketMember]](GetRemoteMember(sri, _)) foreach {
        case None =>
          logger.info(s"tell/sri missing member $sri $user $msg")
          lila.mon.socket.remote.lobby.missingSri()
          send(Out.disconnectSri(sri))
        case Some(member) => controller(member).applyOrElse(typ -> msg, {
          case _ => logger.warn(s"Can't handle $typ")
        }: lila.socket.Handler.Controller)
      }
  }

  private val messagesHandled: Set[String] =
    Set("join", "cancel", "joinSeek", "cancelSeek", "idle", "poolIn", "poolOut", "hookIn", "hookOut")

  remoteSocketApi.subscribe("lobby-in", P.In.baseReader)(handler orElse remoteSocketApi.baseHandler)

  private val send: String => Unit = remoteSocketApi.makeSender("lobby-out").apply _

  bus.subscribeFun('nbMembers, 'nbRounds, 'lobbySocketTell) {
    case lila.socket.actorApi.NbMembers(nb) => send(Out.nbMembers(nb))
    case lila.hub.actorApi.round.NbRounds(nb) => send(Out.nbRounds(nb))
    case LobbySocketTellAll(msg) => send(Out.tellLobby(msg))
    case LobbySocketTellActive(msg) => send(Out.tellLobbyActive(msg))
    case LobbySocketTellSris(sris, msg) => send(Out.tellSris(sris, msg))
  }
}

object LobbyRemoteSocket {

  object Protocol {
    object Out {
      def nbMembers(nb: Int) = s"member/nb $nb"
      def nbRounds(nb: Int) = s"round/nb $nb"
      def tellLobby(payload: JsObject) = s"tell/lobby ${Json stringify payload}"
      def tellLobbyActive(payload: JsObject) = s"tell/lobby ${Json stringify payload}"
      def disconnectSri(sri: Sri) = s"disconnect/sri $sri"
      def tellSris(sris: Iterable[Sri], payload: JsValue) =
        s"tell/sris ${P.Out.commaList(sris)} ${Json stringify payload}"
    }
  }
}
