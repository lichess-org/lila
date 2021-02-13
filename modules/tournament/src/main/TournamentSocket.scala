package lila.tournament

import akka.actor._
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.game.Game
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage
import lila.user.User

final private class TournamentSocket(
    repo: TournamentRepo,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mode: play.api.Mode
) {

  private val allWaitingUsers = new ConcurrentHashMap[Tournament.ID, WaitingUsers.WithNext](64)

  private val reloadThrottler = LateMultiThrottler(executionTimeout = 1.seconds.some, logger = logger)

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
    send(Protocol.Out.getWaitingUsers(RoomId(tour.id), tour.name()(lila.i18n.defaultLang)))
    val promise = Promise[WaitingUsers]()
    allWaitingUsers.compute(
      tour.id,
      (_: Tournament.ID, cur: WaitingUsers.WithNext) =>
        Option(cur).getOrElse(WaitingUsers emptyWithNext tour.clock).copy(next = promise.some)
    )
    promise.future.withTimeout(2.seconds, lila.base.LilaException("getWaitingUsers timeout"))
  }

  def hasUser(tourId: Tournament.ID, userId: User.ID): Boolean =
    Option(allWaitingUsers.get(tourId)).exists(_.waiting hasUser userId)

  def finish(tourId: Tournament.ID): Unit = {
    allWaitingUsers remove tourId
    reload(tourId)
  }

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Tournament)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Tournament(roomId.value).some,
      chatBusChan = _.Tournament,
      localTimeout = Some { (roomId, modId, _) =>
        repo.fetchCreatedBy(roomId.value).map(_ has modId)
      }
    )

  private lazy val tourHandler: Handler = {
    case Protocol.In.WaitingUsers(roomId, users) =>
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
  ) >>- send(P.Out.boot)

  object Protocol {

    object In {

      case class WaitingUsers(roomId: RoomId, userIds: Set[User.ID]) extends P.In

      val reader: P.In.Reader = raw => tourReader(raw) orElse RP.In.reader(raw)

      val tourReader: P.In.Reader = raw =>
        raw.path match {
          case "tour/waiting" =>
            raw.get(2) {
              case Array(roomId, users) =>
                WaitingUsers(RoomId(roomId), P.In.commas(users).toSet).some
            }
          case _ => none
        }
    }

    object Out {
      def getWaitingUsers(roomId: RoomId, name: String) = s"tour/get/waiting $roomId $name"
    }
  }
}
