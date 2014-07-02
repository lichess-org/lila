package lila.pool

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import play.api.libs.json._

import actorApi._
import lila.common.{ LightUser, Debouncer }
import lila.game.actorApi.FinishGame
import lila.game.GameRepo
import lila.hub.actorApi.SendTos
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import lila.user.{ User, UserRepo }

private[pool] final class PoolActor(
    setup: PoolSetup,
    val history: History,
    lightUser: String => Option[LightUser],
    isOnline: String => Boolean,
    renderer: ActorSelection,
    joiner: Joiner,
    uidTimeout: Duration) extends SocketActor[Member](uidTimeout) with Historical[Member] {

  private var pool = Pool(setup, Nil, Nil, DateTime.now plusSeconds 5)

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'finishGame, 'adjustCheater)
    val findSinceMinutes = setup.clock.estimateTotalTime * 2 * 10 / 60
    GameRepo.findRecentPoolGames(setup.id, findSinceMinutes) foreach { games =>
      UserRepo byIds games.filter(_.playable).flatMap(_.userIds) foreach { users =>
        self ! Preload(games, users)
      }
    }
  }

  // last time each user waved to the pool
  private val wavers = new lila.memo.ExpireSetMemo(20 seconds)

  def receiveSpecific = {

    case GetPool => sender ! pool

    case Enter(user) =>
      pool = pool withUser user
      wavers put user.id
      reloadNotifier ! Debouncer.Nothing
      sender ! true

    case Leave(userId) =>
      if (pool.contains(userId)) {
        pool = pool withoutUserId userId
        reloadNotifier ! Debouncer.Nothing
      }
      sender ! true

    case EjectLeavers =>
      pool.players map (_.user.id) filter isOnline foreach wavers.put
      pool.players filterNot (p => wavers get p.user.id) map (_.user.id) map Leave.apply foreach self.!

    case FinishGame(g, Some(white), Some(black)) if g.poolId == Some(setup.id) =>
      GameRepo game g.id foreach {
        _ foreach { game =>
          self ! DoFinishGame(game, white, black)
        }
      }

    case DoFinishGame(game, white, black) =>
      pool = pool finishGame game
      UserRepo byIds List(white.id, black.id) map UpdateUsers.apply foreach self.!

    case UpdateUsers(users) =>
      pool = pool updatePlayers users
      reloadNotifier ! Debouncer.Nothing

    case RemindPlayers =>
      import makeTimeout.short
      renderer ? RemindPool(pool) foreach {
        case html: play.twirl.api.Html =>
          val event = SendTos(pool.players.map(_.user.id).toSet, Json.obj(
            "t" -> "poolReminder",
            "d" -> Json.obj(
              "id" -> pool.setup.id,
              "html" -> html.toString
            )))
          context.system.lilaBus.publish(event, 'users)
      }

    case CheckWave if (pool.nextWaveAt isBefore DateTime.now) =>
      val pairings = AutoPairing(pool, userIds.toSet)
      joiner(pool.setup, pairings) map AddPairings.apply pipeTo self

    case CheckPlayers =>
      val waitingUserIds = userIds.toSet
      val oldPlayers = pool.players
      val newPlayers = oldPlayers map { p =>
        p setWaiting {
          if (pool.isPlaying(p.user.id)) false
          else waitingUserIds(p.user.id)
        }
      }
      if (newPlayers != oldPlayers) {
        pool = pool.copy(players = newPlayers)
        reloadNotifier ! Debouncer.Nothing
      }

    case AddPairings(pairings) =>
      pool = pool.withPairings(pairings.map(_.pairing))
      pool = pool.copy(nextWaveAt = Wave calculateNext pool)
      pairings.map(_.game) foreach { game =>
        game.players foreach { player =>
          player.userId flatMap memberByUserId foreach { member =>
            notifyMember("redirect", game fullIdOf player.color)(member)
          }
        }
      }
      reloadNotifier ! Debouncer.Nothing

    case Preload(games, users) =>
      pool = pool.copy(
        pairings = games flatMap { game =>
          (game.whitePlayer.userId |@| game.blackPlayer.userId) apply {
            case (user1, user2) =>
              Pairing(
                gameId = game.id,
                status = game.status,
                user1 = user1,
                user2 = user2,
                user1RatingDiff = game.whitePlayer.ratingDiff,
                user2RatingDiff = game.blackPlayer.ratingDiff,
                turns = game.turns,
                winner = game.winnerUserId)
          }
        },
        players = users.map { user =>
          Player(user.light, setup.glickoLens(user).intRating, none)
        })
      pool.players map (_.id) foreach wavers.put
      reloadNotifier ! Debouncer.Nothing

    case PingVersion(uid, v) =>
      ping(uid)
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }

    case lila.chat.actorApi.ChatLine(_, line) => line match {
      case line: lila.chat.UserLine =>
        notifyVersionTrollable("message", lila.chat.Line toJson line, troll = line.troll)
      case _ =>
    }

    case GetVersion => sender ! history.version

    case Join(uid, user, version) =>
      import play.api.libs.iteratee._
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      crowdNotifier ! members.values
      sender ! Connected(enumerator, member)

    case Quit(uid) =>
      quit(uid)
      crowdNotifier ! members.values

    case lila.hub.actorApi.mod.MarkCheater(userId) =>
      pool = pool withoutUserId userId
  }

  val crowdNotifier =
    context.system.actorOf(Props(new Debouncer(1.seconds, (ms: Iterable[Member]) => {
      val (anons, users) = ms.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
        case ((anons, users), Some(user)) => anons -> (user :: users)
        case ((anons, users), None)       => (anons + 1) -> users
      }
      notifyVersion("crowd", showSpectators(users, anons))
    })))

  val reloadNotifier =
    context.system.actorOf(Props(new Debouncer(1.seconds, (_: Debouncer.Nothing) => {
      notifyAll(makeMessage("reload"))
    })))

  def notifyVersionTrollable[A: Writes](t: String, data: A, troll: Boolean) {
    val vmsg = history.+=(makeMessage(t, data), troll)
    members.values.foreach(sendMessage(vmsg))
  }
}
