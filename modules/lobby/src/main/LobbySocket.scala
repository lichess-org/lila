package lila.lobby

import ornicar.scalalib.Zero
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._
import lila.game.Pov
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.timeline._
import lila.hub.Trouper
import lila.pool.{ PoolApi, PoolConfig }
import lila.rating.RatingRange
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, Sri, Sris }
import lila.user.{ User, UserRepo }

final class LobbySocket(
    remoteSocketApi: lila.socket.RemoteSocket,
    lobby: LobbyTrouper,
    blocking: User.ID => Fu[Set[User.ID]],
    poolApi: PoolApi,
    system: akka.actor.ActorSystem
) {

  import LobbySocket._
  import Protocol._

  val trouper: Trouper = new Trouper {

    private val members = scala.collection.mutable.AnyRefMap.empty[SriStr, Member]
    private var idleSris = collection.mutable.Set[SriStr]()
    private var hookSubscriberSris = collection.mutable.Set[SriStr]()
    private var removedHookIds = ""

    val process: Trouper.Receive = {

      case GetMember(sri, promise) => promise success members.get(sri.value)

      case GetSrisP(promise) =>
        promise success Sris(members.keySet.map(Sri.apply)(scala.collection.breakOut))
        lila.mon.lobby.socket.idle(idleSris.size)
        lila.mon.lobby.socket.hookSubscribers(hookSubscriberSris.size)

      case Cleanup =>
        idleSris retain members.contains
        hookSubscriberSris retain members.contains

      case Join(member) => members += (member.sri.value -> member)

      case Leave(sri) => quit(sri)
      case LeaveBatch(sris) => sris foreach quit
      case LeaveAll =>
        members.clear()
        idleSris.clear()
        hookSubscriberSris.clear()

      case ReloadTournaments(html) => tellActive(makeMessage("tournaments", html))

      case ReloadSimuls(html) => tellActive(makeMessage("simuls", html))

      // TODO send only to lobby users
      case ReloadTimelines(users) => send(P.Out.tellUsers(users.toSet, makeMessage("reload_timeline")))

      case AddHook(hook) => send(P.Out.tellSris(
        hookSubscriberSris diff idleSris filter { sri =>
          members get sri exists { Biter.showHookTo(hook, _) }
        } map Sri.apply,
        makeMessage("had", hook.render)
      ))

      case RemoveHook(hookId) => removedHookIds = s"$removedHookIds$hookId"

      case SendHookRemovals =>
        if (removedHookIds.nonEmpty) {
          tellActiveHookSubscribers(makeMessage("hrm", removedHookIds))
          removedHookIds = ""
        }
        system.scheduler.scheduleOnce(1249 millis)(this ! SendHookRemovals)

      case JoinHook(sri, hook, game, creatorColor) =>
        lila.mon.lobby.hook.join()
        send(P.Out.tellSri(hook.sri, gameStartRedirect(game pov creatorColor)))
        send(P.Out.tellSri(sri, gameStartRedirect(game pov !creatorColor)))

      case JoinSeek(userId, seek, game, creatorColor) =>
        lila.mon.lobby.seek.join()
        send(Out.tellLobbyUser(seek.user.id, gameStartRedirect(game pov creatorColor)))
        send(Out.tellLobbyUser(userId, gameStartRedirect(game pov !creatorColor)))

      case p: PoolApi.Pairing =>
        send(P.Out.tellSri(p.whiteSri, gameStartRedirect(p.game pov chess.White)))
        send(P.Out.tellSri(p.blackSri, gameStartRedirect(p.game pov chess.Black)))

      case HookIds(ids) => tellActiveHookSubscribers(makeMessage("hli", ids mkString ""))

      case AddSeek(_) | RemoveSeek(_) => tellActive(makeMessage("reload_seeks"))

      case lila.hub.actorApi.streamer.StreamsOnAir(html) => tellActive(makeMessage("streams", html))

      case ChangeFeatured(_, msg) => tellActive(msg)

      case SetIdle(sri, true) => idleSris += sri.value
      case SetIdle(sri, false) => idleSris -= sri.value

      case HookSub(member, false) => hookSubscriberSris -= member.sri.value
      case AllHooksFor(member, hooks) =>
        send(P.Out.tellSri(member.sri, makeMessage("hooks", JsArray(hooks.map(_.render)))))
        hookSubscriberSris += member.sri.value
    }

    system.lilaBus.subscribe(this, 'changeFeaturedGame, 'streams, 'poolGame, 'lobbySocket)
    system.scheduler.scheduleOnce(7 seconds)(this ! SendHookRemovals)
    system.scheduler.schedule(1 minute, 1 minute)(this ! Cleanup)

    private def tellActive(msg: JsObject): Unit = send(Out.tellLobbyActive(msg))

    private def tellActiveHookSubscribers(msg: JsObject): Unit =
      send(P.Out.tellSris(hookSubscriberSris diff idleSris map Sri.apply, msg))

    private def gameStartRedirect(pov: Pov) =
      makeMessage("redirect", Json.obj(
        "id" -> pov.fullId,
        "url" -> s"/${pov.fullId}"
      ).add("cookie" -> lila.game.AnonCookie.json(pov)))

    private def quit(sri: Sri): Unit = {
      members -= sri.value
      idleSris -= sri.value
      hookSubscriberSris -= sri.value
    }
  }

  // solve circular reference
  lobby ! LobbyTrouper.SetSocket(trouper)

  private val poolLimitPerSri = new lila.memo.RateLimit[SriStr](
    credits = 25,
    duration = 1 minute,
    name = "lobby hook/pool per member",
    key = "lobby.hook_pool.member"
  )

  private def HookPoolLimit[A: Zero](member: Member, cost: Int, msg: => String)(op: => A) =
    poolLimitPerSri(k = member.sri.value, cost = cost, msg = msg)(op)

  def controller(member: Member): lila.socket.Handler.Controller = {
    case ("join", o) if !member.bot => HookPoolLimit(member, cost = 5, msg = s"join $o") {
      o str "d" foreach { id =>
        lobby ! BiteHook(id, member.sri, member.user)
      }
    }
    case ("cancel", _) => HookPoolLimit(member, cost = 1, msg = "cancel") {
      lobby ! CancelHook(member.sri)
    }
    case ("joinSeek", o) if !member.bot => HookPoolLimit(member, cost = 5, msg = s"joinSeek $o") {
      for {
        id <- o str "d"
        user <- member.user
      } lobby ! BiteSeek(id, user)
    }
    case ("cancelSeek", o) => HookPoolLimit(member, cost = 1, msg = s"cancelSeek $o") {
      for {
        id <- o str "d"
        user <- member.user
      } lobby ! CancelSeek(id, user)
    }
    case ("idle", o) => trouper ! SetIdle(member.sri, ~(o boolean "d"))
    // entering a pool
    case ("poolIn", o) if !member.bot => HookPoolLimit(member, cost = 1, msg = s"poolIn $o") {
      for {
        user <- member.user
        d <- o obj "d"
        id <- d str "id"
        ratingRange = d str "range" flatMap RatingRange.apply
        blocking = d str "blocking"
      } {
        lobby ! CancelHook(member.sri) // in case there's one...
        poolApi.join(
          PoolConfig.Id(id),
          PoolApi.Joiner(
            userId = user.id,
            sri = member.sri,
            ratingMap = user.perfMap.mapValues(_.rating),
            ratingRange = ratingRange,
            lame = user.lame,
            blocking = user.blocking ++ blocking
          )
        )
      }
    }
    // leaving a pool
    case ("poolOut", o) => HookPoolLimit(member, cost = 1, msg = s"poolOut $o") {
      for {
        id <- o str "d"
        user <- member.user
      } poolApi.leave(PoolConfig.Id(id), user.id)
    }
    // entering the hooks view
    case ("hookIn", _) => HookPoolLimit(member, cost = 2, msg = "hookIn") {
      lobby ! HookSub(member, true)
    }
    // leaving the hooks view
    case ("hookOut", _) => trouper ! HookSub(member, false)
  }

  private def getOrConnect(sri: Sri, userOpt: Option[User.ID]): Fu[Member] =
    trouper.ask[Option[Member]](GetMember(sri, _)) getOrElse {
      userOpt ?? UserRepo.enabledById flatMap { user =>
        (user ?? { u =>
          remoteSocketApi.baseHandler(P.In.ConnectUser(u.id))
          blocking(u.id)
        }) map { blocks =>
          val member = Member(sri, user map { LobbyUser.make(_, blocks) })
          trouper ! Join(member)
          member
        }
      }
    }

  private val handler: Handler = {
    case P.In.ConnectSri(sri, userOpt) => getOrConnect(sri, userOpt)
    case P.In.ConnectSris(cons) => cons foreach {
      case (sri, userId) => getOrConnect(sri, userId)
    }
    case P.In.DisconnectSri(sri) => trouper ! Leave(sri)
    case P.In.DisconnectSris(sris) => trouper ! LeaveBatch(sris)

    case P.In.DisconnectAll =>
      lobby ! LeaveAll
      trouper ! LeaveAll

    case tell @ P.In.TellSri(sri, user, typ, msg) if messagesHandled(typ) =>
      lila.mon.socket.remote.lobby.tellSri(typ)
      getOrConnect(sri, user) foreach { member =>
        controller(member).applyOrElse(typ -> msg, {
          case _ => logger.warn(s"Can't handle $typ")
        }: lila.socket.Handler.Controller)
      }
  }

  private val messagesHandled: Set[String] =
    Set("join", "cancel", "joinSeek", "cancelSeek", "idle", "poolIn", "poolOut", "hookIn", "hookOut")

  remoteSocketApi.subscribe("lobby-in", P.In.baseReader)(handler orElse remoteSocketApi.baseHandler)

  private val send: String => Unit = remoteSocketApi.makeSender("lobby-out").apply _

  system.lilaBus.subscribeFun('nbMembers, 'nbRounds) {
    case lila.socket.actorApi.NbMembers(nb) => send(Out.nbMembers(nb))
    case lila.hub.actorApi.round.NbRounds(nb) => send(Out.nbRounds(nb))
  }
}

private object LobbySocket {

  type SriStr = String

  case class Member(sri: Sri, user: Option[LobbyUser]) {
    def bot = user.exists(_.bot)
    def userId = user.map(_.id)
    def isAuth = userId.isDefined
  }

  object Protocol {
    object Out {
      def nbMembers(nb: Int) = s"member/nb $nb"
      def nbRounds(nb: Int) = s"round/nb $nb"
      def tellLobby(payload: JsObject) = s"tell/lobby ${Json stringify payload}"
      def tellLobbyActive(payload: JsObject) = s"tell/lobby/active ${Json stringify payload}"
      def tellLobbyUser(userId: String, payload: JsObject) = s"tell/lobby/user $userId ${Json stringify payload}"
    }
  }

  case object Cleanup
  case class Join(member: Member)
  case class GetMember(sri: Sri, promise: Promise[Option[Member]])
  object SendHookRemovals
  case class SetIdle(sri: Sri, value: Boolean)
}
