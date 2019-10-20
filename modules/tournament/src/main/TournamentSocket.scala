package lila.tournament

import akka.actor._
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.game.{ Game, Pov }
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage
import lila.user.User

private final class TournamentSocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: ActorSelection,
    system: ActorSystem
) {

  private val allWaitingUsers = new ConcurrentHashMap[Tournament.ID, WaitingUsers.WithNext]

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
    send(Protocol.Out.getRoomUsers(RoomId(tour.id)))
    val promise = Promise[WaitingUsers]
    allWaitingUsers.compute(
      tour.id,
      (_: Tournament.ID, cur: WaitingUsers.WithNext) =>
        Option(cur).getOrElse(WaitingUsers emptyWithNext tour.clock).copy(next = promise.some)
    )
    promise.future.withTimeout(2.seconds, lila.base.LilaException("getWaitingUsers timeout"))(system)
  }

  def hasUser(tourId: Tournament.ID, userId: User.ID): Boolean =
    Option(allWaitingUsers.get(tourId)).exists(_.waiting hasUser userId)

  def finish(tourId: Tournament.ID): Unit = {
    allWaitingUsers remove tourId
    reload(tourId)
  }

  lazy val rooms = makeRoomMap(send, system.lilaBus)

  private lazy val handler: Handler = roomHandler(rooms, chat,
    roomId => _.Tournament(roomId.value).some)

  private lazy val tourHandler: Handler = {
    case Protocol.In.RoomUsers(roomId, users) =>
      allWaitingUsers.computeIfPresent(
        roomId.value,
        (_: Tournament.ID, cur: WaitingUsers.WithNext) => {
          val newWaiting = cur.waiting.update(users)
          cur.next.foreach(_ success newWaiting)
          WaitingUsers.WithNext(newWaiting, none)
        }
      )
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("tour-out").apply _

  remoteSocketApi.subscribe("tour-in", Protocol.In.reader)(
    tourHandler orElse handler orElse remoteSocketApi.baseHandler
  )

  object Protocol {

    object In {

      case class RoomUsers(roomId: RoomId, userIds: Set[User.ID]) extends P.In

      val reader: P.In.Reader = raw => tourReader(raw) orElse RP.In.reader(raw)

      val tourReader: P.In.Reader = raw => raw.path match {
        case "room/users" => raw.get(2) {
          case Array(roomId, users) => RoomUsers(RoomId(roomId), P.In.commas(users).toSet).some
        }
        case _ => none
      }
    }

    object Out {
      def getRoomUsers(roomId: RoomId) = s"room/get/users $roomId"
    }
  }
}
