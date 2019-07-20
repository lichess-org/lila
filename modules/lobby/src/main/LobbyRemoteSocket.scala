package lila.lobby

import play.api.libs.json._

import actorApi._
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.user.{ User, UserRepo }

final class LobbyRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    socket: LobbySocket,
    blocking: User.ID => Fu[Set[User.ID]],
    controller: LobbySocketMember => lila.socket.Handler.Controller,
    bus: lila.common.Bus
) {

  import LobbyRemoteSocket.Protocol._

  private val send: String => Unit = remoteSocketApi.sendTo("lobby-out") _

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

    case tell @ P.In.TellSri(sri, _, typ, msg) if messagesHandled(typ) =>
      socket.ask[Option[LobbyRemoteSocketMember]](GetRemoteMember(sri, _)) foreach {
        case None => logger.warn(s"tell/sri missing member $sri")
        case Some(member) => controller(member).applyOrElse(typ -> msg, {
          case _ => logger.warn(s"Can't handle $typ")
        }: lila.socket.Handler.Controller)
      }
  }

  private val messagesHandled: Set[String] =
    Set("join", "cancel", "joinSeek", "cancelSeek", "idle", "poolIn", "poolOut", "hookIn", "hookOut")

  remoteSocketApi.subscribe("lobby-in", P.In.baseReader)(handler orElse remoteSocketApi.baseHandler)

  bus.subscribeFun('nbMembers, 'nbRounds, 'lobbySocketTellAll) {
    case lila.socket.actorApi.NbMembers(nb) => send(Out.nbMembers(nb))
    case lila.hub.actorApi.round.NbRounds(nb) => send(Out.nbRounds(nb))
    case LobbySocketTellAll(msg) => send(P.Out.tellAll(msg))
  }
}

object LobbyRemoteSocket {

  object Protocol {
    object Out {
      def nbMembers(nb: Int) = s"member/nb $nb"
      def nbRounds(nb: Int) = s"round/nb $nb"
    }
  }
}
