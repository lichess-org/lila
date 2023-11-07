package lila.tournament

import akka.actor.*

import lila.game.Game
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol as RP, * }
import lila.socket.RemoteSocket.{ Protocol as P, * }
import lila.socket.Socket.makeMessage
import lila.common.Json.given

final private class TournamentSocket(
    repo: TournamentRepo,
    waitingUsers: WaitingUsersApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    chat: lila.chat.ChatApi
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler
):

  private val reloadThrottler = LateMultiThrottler(executionTimeout = 1.seconds.some, logger = logger)

  def reload(tourId: TourId): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = tourId,
      run = fuccess:
        send(RP.Out.tellRoom(tourId into RoomId, makeMessage("reload")))
      ,
      delay = 1.seconds.some
    )

  def startGame(tourId: TourId, game: Game): Unit =
    game.players.foreach: player =>
      player.userId.foreach: userId =>
        send:
          RP.Out.tellRoomUser(tourId into RoomId, userId, makeMessage("redirect", game fullIdOf player.color))

  def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] =
    send(Protocol.Out.getWaitingUsers(tour.id into RoomId, tour.name()(using lila.i18n.defaultLang)))
    val promise = Promise[WaitingUsers]()
    waitingUsers.registerNextPromise(tour, promise)
    promise.future.withTimeout(2.seconds, "TournamentSocket.getWaitingUsers")

  def hasUser = waitingUsers.hasUser

  def finish(tourId: TourId): Unit =
    waitingUsers remove tourId
    reload(tourId)

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.Tournament)

  private lazy val handler: Handler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Tournament(roomId into TourId).some,
      chatBusChan = _.Tournament,
      localTimeout = Some: (roomId, modId, _) =>
        repo.fetchCreatedBy(roomId into TourId).map(_ has modId)
    )

  private lazy val tourHandler: Handler = { case Protocol.In.WaitingUsers(roomId, users) =>
    waitingUsers.registerWaitingUsers(roomId into TourId, users)
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("tour-out").apply

  remoteSocketApi.subscribe("tour-in", Protocol.In.reader)(
    tourHandler orElse handler orElse remoteSocketApi.baseHandler
  ) andDo send(P.Out.boot)

  object Protocol:

    object In:

      case class WaitingUsers(roomId: RoomId, userIds: Set[UserId]) extends P.In

      val reader: P.In.Reader = raw => tourReader(raw) orElse RP.In.reader(raw)

      val tourReader: P.In.Reader = raw =>
        raw.path match
          case "tour/waiting" =>
            raw.get(2) { case Array(roomId, users) =>
              WaitingUsers(RoomId(roomId), UserId from P.In.commas(users).toSet).some
            }
          case _ => none

    object Out:
      def getWaitingUsers(roomId: RoomId, name: String) = s"tour/get/waiting $roomId $name"
