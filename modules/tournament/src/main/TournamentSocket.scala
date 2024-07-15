package lila.tournament

import akka.actor.*

import lila.common.Json.given
import lila.common.LateMultiThrottler
import lila.core.socket.{ protocol as P, * }
import lila.room.RoomSocket.{ Protocol as RP, * }

final private class TournamentSocket(
    repo: TournamentRepo,
    waitingUsers: WaitingUsersApi,
    socketKit: SocketKit,
    chat: lila.core.chat.ChatApi
)(using Executor, ActorSystem, Scheduler, lila.core.user.FlairGet, lila.core.i18n.Translator):

  private val reloadThrottler = LateMultiThrottler(executionTimeout = 1.seconds.some, logger = logger)

  def reload(tourId: TourId): Unit =
    reloadThrottler ! LateMultiThrottler.work(
      id = tourId,
      run = fuccess:
        send(RP.Out.tellRoom(tourId.into(RoomId), makeMessage("reload")))
      ,
      delay = 1.seconds.some
    )

  def startGame(tourId: TourId, game: Game): Unit =
    game.players.foreach: player =>
      player.userId.foreach: userId =>
        send:
          RP.Out.tellRoomUser(
            tourId.into(RoomId),
            userId,
            makeMessage("redirect", game.fullIdOf(player.color))
          )

  def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] =
    given play.api.i18n.Lang = lila.core.i18n.defaultLang
    send(Protocol.Out.getWaitingUsers(tour.id.into(RoomId), tour.name()))
    val promise = Promise[WaitingUsers]()
    waitingUsers.registerNextPromise(tour, promise)
    promise.future.withTimeout(2.seconds, "TournamentSocket.getWaitingUsers")

  def hasUser = waitingUsers.hasUser

  def finish(tourId: TourId): Unit =
    waitingUsers.remove(tourId)
    reload(tourId)

  lazy val rooms = makeRoomMap(send)

  subscribeChat(rooms, _.tournament)

  private lazy val handler: SocketHandler =
    roomHandler(
      rooms,
      chat,
      logger,
      roomId => _.Tournament(roomId.into(TourId)).some,
      chatBusChan = _.tournament,
      localTimeout = Some: (roomId, modId, _) =>
        repo.fetchCreatedBy(roomId.into(TourId)).map(_.has(modId))
    )

  private lazy val tourHandler: SocketHandler = { case Protocol.In.WaitingUsers(roomId, users) =>
    waitingUsers.registerWaitingUsers(roomId.into(TourId), users)
  }

  private lazy val send = socketKit.send("tour-out")

  socketKit
    .subscribe("tour-in", Protocol.In.reader.orElse(RP.In.reader))(
      tourHandler.orElse(handler).orElse(socketKit.baseHandler)
    )
    .andDo(send(P.Out.boot))

  object Protocol:

    object In:

      case class WaitingUsers(roomId: RoomId, userIds: Set[UserId]) extends P.In

      val reader: P.In.Reader =
        case P.RawMsg("tour/waiting", raw) =>
          raw.get(2) { case Array(roomId, users) =>
            WaitingUsers(RoomId(roomId), UserId.from(P.In.commas(users).toSet)).some
          }

    object Out:
      def getWaitingUsers(roomId: RoomId, name: String) = s"tour/get/waiting $roomId $name"
