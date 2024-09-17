package lila.tournament

import akka.actor._
import play.api.libs.json.JsObject
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.concurrent.Promise
import org.joda.time.DateTime

import lila.game.Game
import lila.hub.LateMultiThrottler
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.makeMessage
import lila.user.User

final private class TournamentSocket(
    api: TournamentApi,
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

  def reloadUsers(tourId: Tournament.ID, users: List[User.ID]): Unit =
    users foreach { userId =>
      send(RP.Out.tellRoomUser(RoomId(tourId), userId, makeMessage("reload")))
    }

  def startGame(tourId: Tournament.ID, game: Game): Unit = {
    game.players foreach { player =>
      player.userId foreach { userId =>
        send(RP.Out.tellRoomUser(RoomId(tourId), userId, makeMessage("redirect", game fullIdOf player.color)))
      }
    }
    reload(tourId)
  }

  def arrangementChange(arrangement: Arrangement): Unit = {
    arrangement.userIds foreach { userId =>
      send(
        RP.Out.tellRoomUser(
          RoomId(arrangement.tourId),
          userId,
          makeMessage("arrangement", JsonView.arrangement(arrangement))
        )
      )
    }
    reload(arrangement.tourId)
  }

  def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] = {
    send(Protocol.Out.getWaitingUsers(RoomId(tour.id), tour.name()(lila.i18n.defaultLang)))
    val promise = Promise[WaitingUsers]()
    allWaitingUsers.compute(
      tour.id,
      (_: Tournament.ID, cur: WaitingUsers.WithNext) =>
        Option(cur)
          .getOrElse(WaitingUsers.emptyWithNext(tour.timeControl.estimateTotalSeconds))
          .copy(next = promise.some)
    )
    promise.future.withTimeout(5.seconds, lila.base.LilaException("getWaitingUsers timeout"))
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
    roomHandler(rooms, chat, logger, roomId => _.Tournament(roomId.value).some, chatBusChan = _.Tournament)

  private lazy val tourHandler: Handler = {
    case Protocol.In.WaitingUsers(roomId, users) =>
      allWaitingUsers
        .computeIfPresent(
          roomId.value,
          (_: Tournament.ID, cur: WaitingUsers.WithNext) => {
            val newWaiting = cur.waiting.update(users)
            cur.next.foreach(_ success newWaiting)
            WaitingUsers.WithNext(newWaiting, none)
          }
        )
        .unit
    case RP.In.TellRoomSri(tourId, P.In.TellSri(_, userIdOpt, tpe, o)) =>
      tpe match {
        case "arrangement-match" =>
          for {
            userId <- userIdOpt
            d      <- o obj "d"
            lookup <- Protocol.In.readArrangementLookup(tourId.value, d)
            join   <- d boolean "y"
          } api.arrangementMatch(lookup, userId, join)
        case "arrangement-time" =>
          for {
            userId <- userIdOpt
            d      <- o obj "d"
            lookup <- Protocol.In.readArrangementLookup(tourId.value, d)
            dateTime = d.long("t") map { new DateTime(_) }
          } api.arrangementSetTime(lookup, userId, dateTime)
        case "arrangement-organizer" =>
          for {
            userId <- userIdOpt
            d      <- o obj "d"
            lookup <- Protocol.In.readArrangementLookup(tourId.value, d)
            name        = d str "name"
            color       = d.boolean("color").map(shogi.Color.fromSente)
            scheduledAt = d.long("scheduled") map { new DateTime(_) }
            settings    = Arrangement.Settings(name, color, scheduledAt)
          } api.arrangementOrganizerSet(lookup, userId, settings)
        case "process-candidate" =>
          for {
            by     <- userIdOpt
            d      <- o obj "d"
            userId <- d str "u"
            accept <- d boolean "v"
          } api.processCandidate(tourId.value, userId, accept, by)
        case "player-kick" =>
          for {
            by     <- userIdOpt
            d      <- o obj "d"
            toKick <- d str "v"
          } api.ejectPlayerFromTour(tourId.value, toKick, by, denyEntry = true)
        case "close-joining" =>
          for {
            by    <- userIdOpt
            d     <- o obj "d"
            close <- d boolean "v"
          } api.closeJoining(tourId.value, close, by)
      }
  }

  private lazy val send: String => Unit = remoteSocketApi.makeSender("tour-out").apply _

  remoteSocketApi.subscribe("tour-in", Protocol.In.reader)(
    tourHandler orElse handler orElse remoteSocketApi.baseHandler
  ) >>- send(P.Out.boot)

  api.registerSocket(this)

  object Protocol {

    object In {

      case class WaitingUsers(roomId: RoomId, userIds: Set[User.ID]) extends P.In

      def readArrangementLookup(tourId: Tournament.ID, d: JsObject): Option[Arrangement.Lookup] =
        for {
          users <- d.str("users") flatMap { userStr =>
            userStr.toLowerCase.split(";", 2) match {
              case Array(user1, user2) => (user1, user2).some
              case _                   => none
            }
          }
          order = d int "order"
        } yield Arrangement.Lookup(tourId, users, order)

      val reader: P.In.Reader = raw => tourReader(raw) orElse RP.In.reader(raw)

      val tourReader: P.In.Reader = raw =>
        raw.path match {
          case "tour/waiting" => {
            raw.get(2) { case Array(roomId, users) =>
              WaitingUsers(RoomId(roomId), P.In.commas(users).toSet).some
            }
          }
          case _ => none
        }
    }

    object Out {
      def getWaitingUsers(roomId: RoomId, name: String) = {
        s"tour/get/waiting $roomId $name"
      }
    }
  }
}
