package lila.simul

import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.chat.{ Chat, UserLine }
import lila.hub.{ TrouperMap, Trouper }
import lila.socket.History
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, GetVersion, SocketVersion }
import lila.user.User

private final class SimulSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem,
    historyMessageTtl: FiniteDuration,
    trouperTtl: FiniteDuration
) {

  import SimulSocket._

  private final class SimulState extends Trouper {
    private var version = SocketVersion(0)
    private var userIds = Set.empty[User.ID]
    val process: Trouper.Receive = {
      case GetVersion(promise) => promise success version
      case lila.chat.actorApi.ChatLine(chatId, line: UserLine) =>
        version = version.inc
        val msg = makeMessage("message", lila.chat.JsonView(line))
        send(Protocol.Out.tellVersion(chatId.value, msg, version, line.troll))
    }
  }

  private val sockets = new TrouperMap(
    mkTrouper = simulId => new SimulState,
    accessTimeout = trouperTtl
  )

  def versionOf(simulId: Simul.ID): Fu[SocketVersion] =
    sockets.askIfPresentOrZero[SocketVersion](simulId)(GetVersion)

  private val handler: Handler = {
    case Protocol.In.ChatSay(simulId, userId, msg) =>
      val chatId = Chat.Id(simulId)
      val publicSource = lila.hub.actorApi.shutup.PublicSource.Simul(simulId).some
      chat ! lila.chat.actorApi.UserTalk(chatId, userId, msg, publicSource)
    case tell @ P.In.TellSri(sri, user, typ, msg) if messagesHandled(typ) =>
    // lila.mon.socket.remote.lobby.tellSri(typ)
    // getOrConnect(sri, user) foreach { member =>
    //   controller(member).applyOrElse(typ -> msg, {
    //     case _ => logger.warn(s"Can't handle $typ")
    //   }: lila.socket.Handler.Controller)
    // }
  }

  private val messagesHandled: Set[String] =
    Set()

  private val inReader: P.In.Reader = raw => Protocol.In.reader(raw) orElse P.In.baseReader(raw)

  remoteSocketApi.subscribe("simul-in", inReader)(handler orElse remoteSocketApi.baseHandler)

  private lazy val send: String => Unit = remoteSocketApi.makeSender("simul-out").apply _

  system.lilaBus.subscribeFun('remoteSocketChat) {
    case line: lila.chat.actorApi.ChatLine => sockets.tell(line.chatId.value, line)
    case a => println(s"remote socket chat unhandled $a")
  }
}

private object SimulSocket {

  object Protocol {
    object Out {
      def tellVersion(simulId: Simul.ID, payload: JsObject, version: SocketVersion, isTroll: Boolean) =
        s"tell/version $simulId $version $isTroll ${Json stringify payload}"
    }
    object In {
      case class ChatSay(simulId: Simul.ID, userId: User.ID, msg: String) extends P.In

      val reader: P.In.Reader = raw => raw.path match {
        case "chat/say" => raw.args.split(" ", 3) match {
          case Array(simulId, userId, msg) => ChatSay(simulId, userId, msg).some
          case _ => none
        }
        case _ => none
      }
    }
  }
}
