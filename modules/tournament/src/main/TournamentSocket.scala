package lila.tournament

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import java.util.concurrent.ConcurrentHashMap
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._
import lila.game.{ Game, Pov }
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage
import lila.user.User

private final class TournamentRemoteSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  private val allWaitingUsers = new ConcurrentHashMap[Tournament.ID, WaitingUsers.WithRemoteUsers]

  private val reloadThrottler = system.actorOf(Props(new LateMultiThrottler(
    executionTimeout = 1.seconds.some,
    logger = logger
  )))

  def reload(tourId: Tournament.ID): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = tourId,
      run = fuccess {
        send(RP.Out.tellRoom(RoomId(tourId), makeMessage("reload")))
      },
      delay = 1.seconds.some
    )

  def startGame(tourId: Tournament.ID, game: Game): Unit = {
    game.players foreach { player =>
      player.userId foreach { userId =>
        send(RP.Out.tellRoomUser(RoomId(tourId), userId, makeMessage("redirect", game fullIdOf player.color)))
      }
    }
    reload(tourId)
  }

  def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] = {
    val promise = Promise[WaitingUsers]
    allWaitingUsers.compute(tour.id, (_: Tournament.ID, cur: WaitingUsers.WithRemoteUsers) => {
      Option(cur).fold(WaitingUsers.WithRemoteUsers(WaitingUsers.empty, promise)) { w =>
        w.copy(next = promise)
      }
    })
    send(Protocol.Out.getRoomUsers(RoomId(tour.id)))
    promise.future
  }
  // waitingUsers = waitingUsers.update(members.values.flatMap(_.userId)(breakOut), clock)
  // promise success waitingUsers

  def finish(tourId: Tournament.ID): Unit = {
    allWaitingUsers remove tourId
    reload(tourId)
  }

  lazy val rooms = makeRoomMap(send, system.lilaBus)

  private lazy val handler: Handler = roomHandler(rooms, chat,
    roomId => _.Tournament(roomId.value).some)

  private lazy val tourHandler: Handler = {
    case Protocol.In.RoomUsers(roomId, users) =>
      allWaitingUsers.computeIfPresent(roomId.value, (_: Tournament.ID, cur: WaitingUsers.WithRemoteUsers) => {
        val newWaiting = cur.waiting.update(users, none)
        cur.next.success(newWaiting) // TODO tourney clock
        newWaiting
      })
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("tour-out").apply _

  remoteSocketApi.subscribe("tour-in", Protocol.In.reader)(tourHandler orElse handler orElse remoteSocketApi.baseHandler)

  object Protocol {

    object In {

      case class RoomUsers(roomId: RoomId, userIds: Set[User.ID]) extends P.In

      val reader: P.In.Reader = raw => tourReader(raw) orElse RP.In.reader(raw)

      val tourReader: P.In.Reader = raw => raw.path match {
        case "room/users" => raw.args split " " match {
          case Array(roomId, users) => RoomUsers(users split "," toSet).some
          case _ => none
        }
        case _ => none
      }
    }

    object Out {
      def getRoomUsers(roomId: RoomId) = s"room/get/users $roomId"
    }
  }
}
