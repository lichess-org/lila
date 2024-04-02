package lila.lobby

import play.api.libs.json.*

import lila.common.Json.given
import lila.game.Pov
import scalalib.actor.SyncActor
import lila.core.timeline.*
import lila.rating.RatingRange
import lila.core.game.ChangeFeatured
import lila.core.socket.{ protocol as P, * }
import lila.core.pool.PoolConfigId
import lila.user.Me

case class LobbyCounters(members: Int, rounds: Int)

final class LobbySocket(
    biter: Biter,
    perfsRepo: lila.user.UserPerfsRepo,
    userApi: lila.user.UserApi,
    socketKit: SocketKit,
    lobby: LobbySyncActor,
    relationApi: lila.relation.RelationApi,
    poolApi: lila.core.pool.PoolApi
)(using ec: Executor, scheduler: Scheduler):

  import LobbySocket.*
  import Protocol.*
  type SocketController = PartialFunction[(String, JsObject), Unit]

  private var lastCounters = LobbyCounters(0, 0)
  def counters             = lastCounters

  private val actor: SyncActor = new SyncActor:

    private val members = lila.memo.CacheApi.scaffeine
      .expireAfterWrite(1 hour)
      .build[SriStr, Member]()
    private val idleSris           = collection.mutable.Set[SriStr]()
    private val hookSubscriberSris = collection.mutable.Set[SriStr]()
    private val removedHookIds     = new collection.mutable.StringBuilder(1024)

    val process: SyncActor.Receive =

      case GetMember(sri, promise) => promise.success(members.getIfPresent(sri.value))

      case GetSrisP(promise) =>
        promise.success(Sris(members.asMap().keySet.map(Sri(_)).toSet))
        lila.mon.lobby.socket.idle.update(idleSris.size)
        lila.mon.lobby.socket.hookSubscribers.update(hookSubscriberSris.size)

      case Cleanup =>
        val membersMap = members.asMap()
        idleSris.filterInPlace(membersMap.contains)
        hookSubscriberSris.filterInPlace(membersMap.contains)

      case Join(member) => members.put(member.sri.value, member)

      case LeaveBatch(sris) => sris.foreach(quit)
      case LeaveAll =>
        members.invalidateAll()
        idleSris.clear()
        hookSubscriberSris.clear()

      case ReloadTimelines(users) => send(Out.tellLobbyUsers(users, makeMessage("reload_timeline")))

      case AddHook(hook) =>
        send(
          P.Out.tellSris(
            hookSubscriberSris
              .diff(idleSris)
              .withFilter { sri =>
                members.getIfPresent(sri).exists { biter.showHookTo(hook, _) }
              }
              .map { Sri(_) },
            makeMessage("had", hook.render)
          )
        )

      case RemoveHook(hookId) => removedHookIds.append(hookId)

      case SendHookRemovals =>
        if removedHookIds.nonEmpty then
          tellActiveHookSubscribers(makeMessage("hrm", removedHookIds.toString))
          removedHookIds.clear()
        scheduler.scheduleOnce(1249 millis)(this ! SendHookRemovals)

      case JoinHook(sri, hook, game, creatorColor) =>
        lila.mon.lobby.hook.join.increment()
        send(P.Out.tellSri(hook.sri, gameStartRedirect(game.pov(creatorColor))))
        send(P.Out.tellSri(sri, gameStartRedirect(game.pov(!creatorColor))))

      case JoinSeek(userId, seek, game, creatorColor) =>
        lila.mon.lobby.seek.join.increment()
        send(Out.tellLobbyUsers(List(seek.user.id), gameStartRedirect(game.pov(creatorColor))))
        send(Out.tellLobbyUsers(List(userId), gameStartRedirect(game.pov(!creatorColor))))

      case lila.core.pool.Pairings(pairings) => send(Out.pairings(pairings))

      case HookIds(ids) => tellActiveHookSubscribers(makeMessage("hli", ids.mkString("")))

      case AddSeek(_) | RemoveSeek(_) => tellActive(makeMessage("reload_seeks"))

      case ChangeFeatured(msg) => tellActive(msg)

      case SetIdle(sri, true)  => idleSris += sri.value
      case SetIdle(sri, false) => idleSris -= sri.value

      case HookSub(member, false) => hookSubscriberSris -= member.sri.value
      case AllHooksFor(member, hooks) =>
        send(
          P.Out.tellSri(member.sri, makeMessage("hooks", hooks.map(_.render)))
        )
        hookSubscriberSris += member.sri.value

    lila.common.Bus.subscribe(this, "changeFeaturedGame", "streams", "poolPairings", "lobbySocket")
    scheduler.scheduleOnce(7 seconds)(this ! SendHookRemovals)
    scheduler.scheduleWithFixedDelay(31 seconds, 31 seconds)(() => this ! Cleanup)

    private def tellActive(msg: JsObject): Unit = send(Out.tellLobbyActive(msg))

    private def tellActiveHookSubscribers(msg: JsObject): Unit =
      send(P.Out.tellSris(hookSubscriberSris.diff(idleSris).map { Sri(_) }, msg))

    import lila.common.Json.given
    private def gameStartRedirect(pov: Pov) = makeMessage(
      "redirect",
      Json
        .obj(
          "id"  -> pov.fullId,
          "url" -> s"/${pov.fullId}"
        )
        .add("cookie" -> lila.game.AnonCookie.json(pov))
    )

    private def quit(sri: Sri): Unit =
      members.invalidate(sri.value)
      idleSris -= sri.value
      hookSubscriberSris -= sri.value

  end actor

  // solve circular reference
  lobby ! LobbySyncActor.SetSocket(actor)

  private val poolLimitPerSri = lila.memo.RateLimit[SriStr](
    credits = 14,
    duration = 30 seconds,
    key = "lobby.hook_pool.member"
  )

  private def HookPoolLimit(member: Member, cost: Int, msg: => String) =
    poolLimitPerSri.zero[Unit](k = member.sri.value, cost = cost, msg = msg)

  def controller(member: Member): SocketController =
    case ("join", o) if !member.bot =>
      HookPoolLimit(
        member,
        cost = if member.isAuth then 4 else 5,
        msg = s"join $o ${member.userId | "anon"}"
      ) {
        o.str("d").foreach { id =>
          lobby ! BiteHook(id, member.sri, member.user)
        }
      }
    case ("cancel", _) =>
      HookPoolLimit(member, cost = 1, msg = "cancel") {
        lobby ! CancelHook(member.sri)
      }
    case ("joinSeek", o) if !member.bot =>
      HookPoolLimit(member, cost = 5, msg = s"joinSeek $o") {
        for
          id   <- o.str("d")
          user <- member.user
        do lobby ! BiteSeek(id, user)
      }
    case ("cancelSeek", o) =>
      HookPoolLimit(member, cost = 1, msg = s"cancelSeek $o") {
        for
          id   <- o.str("d")
          user <- member.user
        do lobby ! CancelSeek(id, user)
      }
    case ("idle", o) => actor ! SetIdle(member.sri, ~(o.boolean("d")))
    // entering a pool
    case ("poolIn", o) if !member.bot =>
      HookPoolLimit(member, cost = 1, msg = s"poolIn $o") {
        for
          user     <- member.user
          d        <- o.obj("d")
          id       <- d.str("id")
          perfType <- poolApi.poolPerfKeys.get(PoolConfigId(id))
          ratingRange = d.str("range").flatMap(RatingRange.parse)
          blocking    = d.get[UserId]("blocking")
        yield
          lobby ! CancelHook(member.sri) // in case there's one...
          perfsRepo.glicko(user.id, perfType).foreach { glicko =>
            poolApi.join(
              PoolConfigId(id),
              lila.core.pool.Joiner(
                sri = member.sri,
                rating = glicko.establishedIntRating | IntRating(
                  scalalib.Maths.boxedNormalDistribution(glicko.intRating.value, glicko.intDeviation, 0.3)
                ),
                ratingRange = ratingRange,
                lame = user.lame,
                blocking = user.blocking.map(_ ++ blocking)
              )(using user.id.into(Me.Id))
            )
          }
      }
    // leaving a pool
    case ("poolOut", o) =>
      HookPoolLimit(member, cost = 1, msg = s"poolOut $o"):
        for
          id   <- o.str("d")
          user <- member.user
        do poolApi.leave(PoolConfigId(id), user.id)
    // entering the hooks view
    case ("hookIn", _) =>
      HookPoolLimit(member, cost = 2, msg = "hookIn"):
        lobby ! HookSub(member, value = true)
    // leaving the hooks view
    case ("hookOut", _) => actor ! HookSub(member, value = false)

  private def getOrConnect(sri: Sri, userOpt: Option[UserId]): Fu[Member] =
    actor.ask[Option[Member]](GetMember(sri, _)).getOrElse {
      userOpt.so(userApi.withPerfs).flatMap { user =>
        user
          .filter(_.enabled.yes)
          .so: u =>
            socketKit.baseHandler(P.In.ConnectUser(u.id))
            relationApi.fetchBlocking(u.id)
          .map: blocks =>
            val member = Member(sri, user.map { LobbyUser.make(_, lila.core.pool.Blocking(blocks)) })
            actor ! Join(member)
            member
      }
    }

  private val handler: SocketHandler =

    case P.In.ConnectSris(cons) =>
      cons.foreach { case (sri, userId) =>
        getOrConnect(sri, userId)
      }

    case P.In.DisconnectSris(sris) => actor ! LeaveBatch(sris)

    case P.In.TellSri(sri, user, tpe, msg) if messagesHandled(tpe) =>
      getOrConnect(sri, user).foreach { member =>
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
      actor ! LeaveAll

  private val messagesHandled: Set[String] =
    Set("join", "cancel", "joinSeek", "cancelSeek", "idle", "poolIn", "poolOut", "hookIn", "hookOut")

  socketKit.subscribe("lobby-in", In.reader)(handler.orElse(socketKit.baseHandler))

  private val send = socketKit.send("lobby-out")

private object LobbySocket:

  type SriStr = String

  case class Member(sri: Sri, user: Option[LobbyUser]):
    def bot    = user.exists(_.bot)
    def userId = user.map(_.id)
    def isAuth = userId.isDefined

  object Protocol:
    object In:
      case class Counters(members: Int, rounds: Int) extends P.In
      val reader: P.In.Reader =
        case P.RawMsg("counters", raw) =>
          raw.get(2) { case Array(m, r) =>
            (m.toIntOption, r.toIntOption).mapN(Counters.apply)
          }
    object Out:
      def pairings(pairings: List[lila.core.pool.Pairing]) =
        val redirs = for
          pairing <- pairings
          color   <- chess.Color.all
          sri    = pairing.players(color)._1
          fullId = pairing.players(color)._2
        yield s"$sri:$fullId"
        s"lobby/pairings ${P.Out.commas(redirs)}"
      def tellLobby(payload: JsObject)       = s"tell/lobby ${Json.stringify(payload)}"
      def tellLobbyActive(payload: JsObject) = s"tell/lobby/active ${Json.stringify(payload)}"
      def tellLobbyUsers(userIds: Iterable[UserId], payload: JsObject) =
        s"tell/lobby/users ${P.Out.commas(userIds)} ${Json.stringify(payload)}"

  case object Cleanup
  case class Join(member: Member)
  case class GetMember(sri: Sri, promise: Promise[Option[Member]])
  object SendHookRemovals
  case class SetIdle(sri: Sri, value: Boolean)
