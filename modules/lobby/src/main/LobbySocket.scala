package lila.lobby

import actorApi._
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.game.Pov
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.timeline._
import lila.hub.Trouper
import lila.i18n.defaultLang
import lila.pool.{ PoolApi, PoolConfig }
import lila.rating.RatingRange
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ makeMessage, Sri, Sris }
import lila.user.User

case class LobbyCounters(members: Int, rounds: Int)

final class LobbySocket(
    biter: Biter,
    userRepo: lila.user.UserRepo,
    remoteSocketApi: lila.socket.RemoteSocket,
    lobby: LobbyTrouper,
    relationApi: lila.relation.RelationApi,
    poolApi: PoolApi,
    system: akka.actor.ActorSystem
)(implicit ec: scala.concurrent.ExecutionContext) {

  import LobbySocket._
  import Protocol._
  type SocketController = PartialFunction[(String, JsObject), Unit]

  private var lastCounters = LobbyCounters(0, 0)
  def counters             = lastCounters

  val trouper: Trouper = new Trouper {

    private val members            = scala.collection.mutable.AnyRefMap.empty[SriStr, Member]
    private val idleSris           = collection.mutable.Set[SriStr]()
    private val hookSubscriberSris = collection.mutable.Set[SriStr]()
    private val removedHookIds     = new collection.mutable.StringBuilder(1024)

    val process: Trouper.Receive = {

      case GetMember(sri, promise) => promise success members.get(sri.value)

      case GetSrisP(promise) =>
        promise success Sris(members.keySet.view.map(Sri.apply).toSet)
        lila.mon.lobby.socket.idle.update(idleSris.size)
        lila.mon.lobby.socket.hookSubscribers.update(hookSubscriberSris.size).unit

      case Cleanup =>
        idleSris filterInPlace members.contains
        hookSubscriberSris filterInPlace members.contains

      case Join(member) => members += (member.sri.value -> member)

      case LeaveBatch(sris) => sris foreach quit
      case LeaveAll =>
        members.clear()
        idleSris.clear()
        hookSubscriberSris.clear()

      case ReloadTimelines(users) => send(Out.tellLobbyUsers(users, makeMessage("reload_timeline")))

      case AddHook(hook) =>
        send(
          P.Out.tellSris(
            hookSubscriberSris diff idleSris withFilter { sri =>
              members get sri exists { biter.showHookTo(hook, _) }
            } map Sri.apply,
            makeMessage("had", hook.render(defaultLang))
          )
        )

      case RemoveHook(hookId) => removedHookIds.append(hookId).unit

      case SendHookRemovals =>
        if (removedHookIds.nonEmpty) {
          tellActiveHookSubscribers(makeMessage("hrm", removedHookIds.toString))
          removedHookIds.clear()
        }
        system.scheduler.scheduleOnce(1249 millis)(this ! SendHookRemovals).unit

      case JoinHook(sri, hook, game, creatorColor) =>
        lila.mon.lobby.hook.join.increment()
        send(P.Out.tellSri(hook.sri, gameStartRedirect(game pov creatorColor)))
        send(P.Out.tellSri(sri, gameStartRedirect(game pov !creatorColor)))

      case JoinSeek(userId, seek, game, creatorColor) =>
        lila.mon.lobby.seek.join.increment()
        send(Out.tellLobbyUsers(List(seek.user.id), gameStartRedirect(game pov creatorColor)))
        send(Out.tellLobbyUsers(List(userId), gameStartRedirect(game pov !creatorColor)))

      case PoolApi.Pairings(pairings) => send(Out.pairings(pairings))

      case HookIds(ids) => tellActiveHookSubscribers(makeMessage("hli", ids mkString ""))

      case AddSeek(_) | RemoveSeek(_) => tellActive(makeMessage("reload_seeks"))

      case ChangeFeatured(_, msg) => tellActive(msg)

      case SetIdle(sri, true)  => idleSris += sri.value
      case SetIdle(sri, false) => idleSris -= sri.value

      case HookSub(member, false) => hookSubscriberSris -= member.sri.value
      case AllHooksFor(member, hooks) =>
        send(
          P.Out.tellSri(member.sri, makeMessage("hooks", JsArray(hooks.map(_.render(defaultLang)))))
        )
        hookSubscriberSris += member.sri.value
    }

    lila.common.Bus.subscribe(this, "changeFeaturedGame", "streams", "poolPairings", "lobbySocket")
    system.scheduler.scheduleOnce(7 seconds)(this ! SendHookRemovals)
    system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute)(() => this ! Cleanup)

    private def tellActive(msg: JsObject): Unit = send(Out.tellLobbyActive(msg))

    private def tellActiveHookSubscribers(msg: JsObject): Unit =
      send(P.Out.tellSris(hookSubscriberSris diff idleSris map Sri.apply, msg))

    private def gameStartRedirect(pov: Pov) =
      makeMessage(
        "redirect",
        Json
          .obj(
            "id"  -> pov.fullId,
            "url" -> s"/${pov.fullId}"
          )
          .add("cookie" -> lila.game.AnonCookie.json(pov))
      )

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
    key = "lobby.hook_pool.member"
  )

  private def HookPoolLimit(member: Member, cost: Int, msg: => String)(op: => Unit) =
    poolLimitPerSri(k = member.sri.value, cost = cost, msg = msg)(op) {}

  def controller(member: Member): SocketController = {
    case ("join", o) if !member.bot =>
      HookPoolLimit(member, cost = 5, msg = s"join $o ${member.userId}") {
        o str "d" foreach { id =>
          lobby ! BiteHook(id, member.sri, member.user)
        }
      }
    case ("cancel", _) =>
      HookPoolLimit(member, cost = 1, msg = "cancel") {
        lobby ! CancelHook(member.sri)
      }
    case ("joinSeek", o) if !member.bot =>
      HookPoolLimit(member, cost = 5, msg = s"joinSeek $o") {
        for {
          id   <- o str "d"
          user <- member.user
        } lobby ! BiteSeek(id, user)
      }
    case ("cancelSeek", o) =>
      HookPoolLimit(member, cost = 1, msg = s"cancelSeek $o") {
        for {
          id   <- o str "d"
          user <- member.user
        } lobby ! CancelSeek(id, user)
      }
    case ("idle", o) => trouper ! SetIdle(member.sri, ~(o boolean "d"))
    // entering a pool
    case ("poolIn", o) if !member.bot =>
      HookPoolLimit(member, cost = 1, msg = s"poolIn $o") {
        for {
          user     <- member.user
          d        <- o obj "d"
          id       <- d str "id"
          perfType <- poolApi.poolPerfTypes get PoolConfig.Id(id)
          ratingRange = d str "range" flatMap RatingRange.apply
          blocking    = d str "blocking"
        } {
          lobby ! CancelHook(member.sri) // in case there's one...
          userRepo.glicko(user.id, perfType) foreach { glicko =>
            poolApi.join(
              PoolConfig.Id(id),
              PoolApi.Joiner(
                userId = user.id,
                sri = member.sri,
                rating = glicko.establishedIntRating |
                  lila.common.Maths.boxedNormalDistribution(glicko.intRating, glicko.intDeviation, 0.3),
                ratingRange = ratingRange,
                lame = user.lame,
                blocking = user.blocking ++ blocking
              )
            )
          }
        }
      }
    // leaving a pool
    case ("poolOut", o) =>
      HookPoolLimit(member, cost = 1, msg = s"poolOut $o") {
        for {
          id   <- o str "d"
          user <- member.user
        } poolApi.leave(PoolConfig.Id(id), user.id)
      }
    // entering the hooks view
    case ("hookIn", _) =>
      HookPoolLimit(member, cost = 2, msg = "hookIn") {
        lobby ! HookSub(member, value = true)
      }
    // leaving the hooks view
    case ("hookOut", _) => trouper ! HookSub(member, value = false)
  }

  private def getOrConnect(sri: Sri, userOpt: Option[User.ID]): Fu[Member] =
    trouper.ask[Option[Member]](GetMember(sri, _)) getOrElse {
      userOpt ?? userRepo.enabledById flatMap { user =>
        (user ?? { u =>
          remoteSocketApi.baseHandler(P.In.ConnectUser(u.id))
          relationApi.fetchBlocking(u.id)
        }) map { blocks =>
          val member = Member(sri, user map { LobbyUser.make(_, blocks) })
          trouper ! Join(member)
          member
        }
      }
    }

  private val handler: Handler = {

    case P.In.ConnectSris(cons) =>
      cons foreach { case (sri, userId) =>
        getOrConnect(sri, userId)
      }

    case P.In.DisconnectSris(sris) => trouper ! LeaveBatch(sris)

    case P.In.TellSri(sri, user, tpe, msg) if messagesHandled(tpe) =>
      getOrConnect(sri, user) foreach { member =>
        controller(member).applyOrElse(
          tpe -> msg,
          { case _ =>
            logger.warn(s"Can't handle $tpe")
          }: SocketController
        )
      }

    case In.Counters(m, r) => lastCounters = LobbyCounters(m, r)

    case P.In.WsBoot =>
      logger.warn("Remote socket boot")
      lobby ! LeaveAll
      trouper ! LeaveAll
  }

  private val messagesHandled: Set[String] =
    Set("join", "cancel", "joinSeek", "cancelSeek", "idle", "poolIn", "poolOut", "hookIn", "hookOut")

  remoteSocketApi.subscribe("lobby-in", In.reader)(handler orElse remoteSocketApi.baseHandler)

  private val send: String => Unit = remoteSocketApi.makeSender("lobby-out").apply _
}

private object LobbySocket {

  type SriStr = String

  case class Member(sri: Sri, user: Option[LobbyUser]) {
    def bot    = user.exists(_.bot)
    def userId = user.map(_.id)
    def isAuth = userId.isDefined
  }

  object Protocol {
    object In {
      case class Counters(members: Int, rounds: Int) extends P.In

      val reader: P.In.Reader = raw =>
        raw.path match {
          case "counters" =>
            import cats.implicits._
            raw.get(2) { case Array(m, r) =>
              (m.toIntOption, r.toIntOption).mapN(Counters)
            }
          case _ => P.In.baseReader(raw)
        }
    }
    object Out {
      def pairings(pairings: List[PoolApi.Pairing]) = {
        val redirs = for {
          pairing <- pairings
          color   <- chess.Color.all
          sri    = pairing sri color
          fullId = pairing.game fullIdOf color
        } yield s"$sri:$fullId"
        s"lobby/pairings ${P.Out.commas(redirs)}"
      }
      def tellLobby(payload: JsObject)       = s"tell/lobby ${Json stringify payload}"
      def tellLobbyActive(payload: JsObject) = s"tell/lobby/active ${Json stringify payload}"
      def tellLobbyUsers(userIds: Iterable[User.ID], payload: JsObject) =
        s"tell/lobby/users ${P.Out.commas(userIds)} ${Json stringify payload}"
    }
  }

  case object Cleanup
  case class Join(member: Member)
  case class GetMember(sri: Sri, promise: Promise[Option[Member]])
  object SendHookRemovals
  case class SetIdle(sri: Sri, value: Boolean)
}
