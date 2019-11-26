package lila.round

import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._, round._
import chess.{ Color, White, Black, Speed }
import lila.chat.Chat
import lila.common.Bus
import lila.game.actorApi.UserStartGame
import lila.game.Game.{ PlayerId, FullId }
import lila.game.{ Game, Progress, Pov, Event, Source, Player => GamePlayer }
import lila.hub.actorApi.DeployPost
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.{ FishnetPlay, FishnetStart, BotPlay, RematchYes, RematchNo, Abort, Resign, IsOnGame }
import lila.hub.Duct
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.socket.UserLagCache
import lila.user.User

private[round] final class RoundDuct(
    dependencies: RoundDuct.Dependencies,
    gameId: Game.ID,
    socketSend: String => Unit
)(implicit proxy: GameProxy) extends Duct {

  import RoundSocket.Protocol
  import RoundDuct._
  import dependencies._

  private var takebackSituation: Option[TakebackSituation] = None

  private var version = SocketVersion(0)

  private var mightBeSimul = true // until proven false
  private var gameSpeed: Option[Speed] = none
  private var chatIds = ChatIds(
    priv = Left(Chat.Id(gameId)), // until replaced with tourney/simul chat
    pub = Chat.Id(s"$gameId/w")
  )

  private final class Player(color: Color) {

    private var offlineSince: Option[Long] = nowMillis.some
    // wether the player closed the window intentionally
    private var bye: Boolean = false
    // connected as a bot
    private var botConnected: Boolean = false

    var userId = none[User.ID]
    var goneWeight = 1f

    def isOnline = offlineSince.isEmpty || botConnected

    def setOnline(on: Boolean): Unit = {
      isLongGone foreach { _ ?? notifyGone(color, false) }
      offlineSince = if (on) None else offlineSince orElse nowMillis.some
      bye = bye && !on
    }
    def setBye: Unit = {
      bye = true
    }

    private def isHostingSimul: Fu[Boolean] = mightBeSimul ?? userId ?? isSimulHost

    private def timeoutMillis = {
      if (bye) RoundSocket.ragequitTimeout.toMillis else RoundSocket.gameDisconnectTimeout(gameSpeed).toMillis
    } * goneWeight atLeast 12000

    def isLongGone: Fu[Boolean] = {
      !botConnected && offlineSince.exists(_ < (nowMillis - timeoutMillis))
    } ?? !isHostingSimul

    def setBotConnected(v: Boolean) =
      botConnected = v

    def isBotConnected = botConnected
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  def getGame: Fu[Option[Game]] = proxy.game

  val process: Duct.ReceiveAsync = {

    // socket stuff

    case ByePlayer(playerId) => proxy playerPov playerId.value map {
      _ foreach { pov =>
        getPlayer(pov.color).setBye
      }
    }

    case GetVersion(promise) => fuccess {
      promise success version
    }
    case SetVersion(v) => fuccess {
      version = v
    }

    case RoomCrowd(white, black) => fuccess {
      whitePlayer setOnline white
      blackPlayer setOnline black
    }

    case IsOnGame(color, promise) => fuccess {
      promise success getPlayer(color).isOnline
    }

    case GetSocketStatus(promise) =>
      whitePlayer.isLongGone zip blackPlayer.isLongGone map {
        case (whiteIsGone, blackIsGone) => promise success SocketStatus(
          version = version,
          whiteOnGame = whitePlayer.isOnline,
          whiteIsGone = whiteIsGone,
          blackOnGame = blackPlayer.isOnline,
          blackIsGone = blackIsGone
        )
      }

    case HasUserId(userId, promise) => fuccess {
      promise success {
        (whitePlayer.userId.has(userId) && whitePlayer.isOnline) ||
          (blackPlayer.userId.has(userId) && blackPlayer.isOnline)
      }
    }

    case SetGameInfo(game, (whiteGoneWeight, blackGoneWeight)) => fuccess {
      whitePlayer.userId = game.player(White).userId
      blackPlayer.userId = game.player(Black).userId
      mightBeSimul = game.isSimul
      chatIds = chatIds update game
      gameSpeed = game.speed.some
      whitePlayer.goneWeight = whiteGoneWeight
      blackPlayer.goneWeight = blackGoneWeight
      buscriptions.chat
    }

    case lila.chat.actorApi.ChatLine(chatId, line) => fuccess {
      publish(List(line match {
        case l: lila.chat.UserLine => Event.UserMessage(l, chatId == chatIds.pub)
        case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
      }))
    }
    case lila.chat.actorApi.OnTimeout(userId) => fuccess {
      socketSend(RP.Out.tellRoom(roomId, makeMessage("chat_timeout", userId)))
    }
    case lila.chat.actorApi.OnReinstate(userId) => fuccess {
      socketSend(RP.Out.tellRoom(roomId, makeMessage("chat_reinstate", userId)))
    }

    case Protocol.In.PlayerChatSay(_, Right(color), msg) => fuccess {
      chatIds.priv.left.toOption foreach { messenger.owner(_, color, msg) }
    }
    case Protocol.In.PlayerChatSay(_, Left(userId), msg) => fuccess(chatIds.priv match {
      case Left(chatId) => messenger.owner(chatId, userId, msg)
      case Right(setup) => messenger.external(setup, userId, msg)
    })

    case Protocol.In.HoldAlert(fullId, ip, mean, sd) => handle(fullId.playerId) { pov =>
      lila.game.GameRepo hasHoldAlert pov flatMap {
        case true => funit
        case false =>
          lila.log("cheat").info(s"hold alert $ip https://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
          lila.mon.cheat.holdAlert()
          proxy.persist(_.setHoldAlert(pov, GamePlayer.HoldAlert(ply = pov.game.turns, mean = mean, sd = sd)).void)
      } inject Nil
    }

    case Protocol.In.UserTv(_, userId) => fuccess {
      buscriptions tv userId
    }

    case UserStartGame(userId, _) => fuccess {
      socketSend(Protocol.Out.userTvNewGame(Game.Id(gameId), userId))
    }

    case a: lila.analyse.actorApi.AnalysisProgress => fuccess {
      socketSend(RP.Out.tellRoom(roomId, makeMessage("analysisProgress", Json.obj(
        "analysis" -> lila.analyse.JsonView.bothPlayers(a.game, a.analysis),
        "tree" -> TreeBuilder(
          id = a.analysis.id,
          pgnMoves = a.game.pgnMoves,
          variant = a.variant,
          analysis = a.analysis.some,
          initialFen = a.initialFen,
          withFlags = JsonView.WithFlags(),
          clocks = none
        )
      ))))
    }

    // round stuff

    case p: HumanPlay => handleHumanPlay(p) { pov =>
      if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
      else {
        recordLag(pov)
        player.human(p, this)(pov)
      }
    } >>- {
      p.trace.finish()
      lila.mon.round.move.full.count()
    }

    case p: BotPlay => handleBotPlay(p) { pov =>
      if (pov.game.outoftime(withGrace = true)) finisher.outOfTime(pov.game)
      else player.bot(p, this)(pov)
    }

    case FishnetPlay(uci, ply) => handle { game =>
      player.fishnet(game, ply, uci, this)
    } >>- lila.mon.round.move.full.count()

    case Abort(playerId) => handle(PlayerId(playerId)) { pov =>
      pov.game.abortable ?? finisher.abort(pov)
    }

    case Resign(playerId) => handle(PlayerId(playerId)) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignAi => handleAi(proxy.game) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case GoBerserk(color) => handle(color) { pov =>
      pov.game.goBerserk(color) ?? { progress =>
        proxy.save(progress) >> proxy.persist(_ goBerserk pov) inject progress.events
      }
    }

    case ResignForce(playerId) => handle(playerId) { pov =>
      (pov.game.resignable && !pov.game.hasAi && pov.game.hasClock && !pov.isMyTurn && pov.forceResignable) ?? {
        getPlayer(!pov.color).isLongGone flatMap {
          case true if !pov.game.variant.insufficientWinningMaterial(pov.game.board, pov.color) => finisher.rageQuit(pov.game, Some(pov.color))
          case true => finisher.rageQuit(pov.game, None)
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    case DrawForce(playerId) => handle(playerId) { pov =>
      (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock) ?? {
        getPlayer(!pov.color).isLongGone flatMap {
          case true => finisher.rageQuit(pov.game, None)
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    // checks if any player can safely (grace) be flagged
    case QuietFlag => handle { game =>
      game.outoftime(withGrace = true) ?? finisher.outOfTime(game)
    }

    // flags a specific player, possibly without grace if self
    case ClientFlag(color, from) => handle { game =>
      (game.turnColor == color) ?? {
        val toSelf = from has PlayerId(game.player(color).id)
        game.outoftime(withGrace = !toSelf) ?? finisher.outOfTime(game)
      }
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    case Abandon => fuccess {
      proxy withGame { game =>
        game.abandoned ?? {
          if (game.abortable) finisher.other(game, _.Aborted, None)
          else finisher.other(game, _.Resign, Some(!game.player.color))
        }
      }
    }

    case DrawYes(playerId) => handle(playerId)(drawer.yes)
    case DrawNo(playerId) => handle(playerId)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case Cheat(color) => handle { game =>
      (game.playable && !game.imported) ?? {
        finisher.other(game, _.Cheat, Some(!color))
      }
    }
    case TooManyPlies => handle(drawer force _)

    case Threefold => proxy withGame { game =>
      drawer autoThreefold game map {
        _ foreach { pov =>
          this ! DrawClaim(PlayerId(pov.player.id))
        }
      }
    }

    case RematchYes(playerId) => handle(PlayerId(playerId))(rematcher.yes)
    case RematchNo(playerId) => handle(PlayerId(playerId))(rematcher.no)

    case TakebackYes(playerId) => handle(playerId) { pov =>
      takebacker.yes(~takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation.some
          events
      }
    }
    case TakebackNo(playerId) => handle(playerId) { pov =>
      takebacker.no(~takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation.some
          events
      }
    }

    case Moretime(playerId) => handle(playerId) { pov =>
      moretimer(pov) flatMap {
        _ ?? { progress =>
          proxy save progress inject progress.events
        }
      }
    }

    case ForecastPlay(lastMove) => handle { game =>
      forecastApi.nextMove(game, lastMove) map { mOpt =>
        mOpt foreach { move =>
          this ! HumanPlay(PlayerId(game.player.id), move, false)
        }
        Nil
      }
    }

    case DeployPost => handle { game =>
      game.playable ?? {
        val freeTime = 20.seconds
        messenger.system(game, (_.untranslated("Lichess has been updated! Sorry for the inconvenience.")))
        val progress = moretimer.give(game, Color.all, freeTime)
        proxy save progress inject progress.events
      }
    }

    case AbortForMaintenance => handle { game =>
      messenger.system(game, (_.untranslated("Game aborted for server maintenance. Sorry for the inconvenience!")))
      game.playable ?? finisher.other(game, _.Aborted, None)
    }

    case AbortForce => handle { game =>
      game.playable ?? finisher.other(game, _.Aborted, None)
    }

    case NoStart => handle { game =>
      game.timeBeforeExpiration.exists(_.centis == 0) ?? finisher.noStart(game)
    }

    case FishnetStart => proxy.game map {
      _.filter(_.playableByAi) foreach {
        player.requestFishnet(_, this)
      }
    }

    case Tick => getGame map { g =>
      if (g.exists(_.forceResignable)) Color.all.foreach { c =>
        if (!getPlayer(c).isOnline) getPlayer(c).isLongGone foreach { _ ?? notifyGone(c, true) }
      }
    }

    case Stop => fuccess {
      if (buscriptions.started) {
        buscriptions.unsubAll
        socketSend(RP.Out.stop(roomId))
      }
    }
  }

  private object buscriptions {

    private var classifiers = collection.mutable.Set.empty[Symbol]

    private def sub(classifier: Symbol) =
      if (!classifiers(classifier)) {
        Bus.subscribe(RoundDuct.this, classifier)
        classifiers += classifier
      }

    def started = classifiers.nonEmpty

    def unsubAll = {
      Bus.unsubscribe(RoundDuct.this, classifiers)
      classifiers.clear
    }

    def tv(userId: User.ID): Unit = sub(Symbol(s"userStartGame:$userId"))

    def chat = chatIds.allIds foreach { chatId =>
      sub(lila.chat.Chat classify chatId)
    }
  }

  private def getPlayer(color: Color): Player = color.fold(whitePlayer, blackPlayer)

  private def recordLag(pov: Pov): Unit =
    if ((pov.game.playedTurns & 30) == 10) {
      // Triggers every 32 moves starting on ply 10.
      // i.e. 10, 11, 42, 43, 74, 75, ...
      for {
        user <- pov.player.userId
        clock <- pov.game.clock
        lag <- clock.lag(pov.color).lagMean
      } UserLagCache.put(user, lag)
    }

  private def notifyGone(color: Color, gone: Boolean): Unit = proxy pov color foreach {
    _ foreach { notifyGone(_, gone) }
  }
  private def notifyGone(pov: Pov, gone: Boolean): Unit =
    socketSend(Protocol.Out.gone(FullId(pov.fullId), gone))

  private def handle[A](op: Game => Fu[Events]): Funit =
    handleGame(proxy.game)(op)

  private def handle(playerId: PlayerId)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy playerPov playerId.value)(op)

  private def handleHumanPlay(p: HumanPlay)(op: Pov => Fu[Events]): Funit =
    handlePov {
      p.trace.segment("fetch", "db") {
        proxy playerPov p.playerId.value
      }
    }(op)

  private def handleBotPlay(p: BotPlay)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy playerPov p.playerId)(op)

  private def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy pov color)(op)

  private def handlePov(pov: Fu[Option[Pov]])(op: Pov => Fu[Events]): Funit =
    pov flatten "pov not found" flatMap { p =>
      (if (p.player.isAi) fufail(s"player $p can't play AI") else op(p)) map publish
    } recover errorHandler("handlePov")

  private def handleAi(game: Fu[Option[Game]])(op: Pov => Fu[Events]): Funit =
    game.map(_.flatMap(_.aiPov)) flatten "pov not found" flatMap op map publish recover errorHandler("handleAi")

  private def handleGame(game: Fu[Option[Game]])(op: Game => Fu[Events]): Funit =
    game flatten "game not found" flatMap op map publish recover errorHandler("handleGame")

  private def publish[A](events: Events): Unit =
    if (events.nonEmpty) {
      events map { e =>
        version = version.inc
        socketSend {
          Protocol.Out.tellVersion(roomId, version, e)
        }
      }
      if (events exists {
        case e: Event.Move => e.threefold
        case _ => false
      }) this ! Threefold
    }

  private def errorHandler(name: String): PartialFunction[Throwable, Unit] = {
    case e: ClientError =>
      logger.info(s"Round client error $name: ${e.getMessage}")
      lila.mon.round.error.client()
    case e: FishnetError =>
      logger.info(s"Round fishnet error $name: ${e.getMessage}")
      lila.mon.round.error.fishnet()
    case e: Exception => logger.warn(s"$name: ${e.getMessage}")
  }

  def roomId = RoomId(gameId)
}

object RoundDuct {

  case class HasUserId(userId: User.ID, promise: Promise[Boolean])
  case class SetGameInfo(game: lila.game.Game, goneWeights: (Float, Float))
  case object Tick
  case object Stop

  case class ChatIds(priv: Either[Chat.Id, Chat.Setup], pub: Chat.Id) {
    def allIds = Seq(priv.fold(identity, _.id), pub)
    def update(g: Game) = {
      g.tournamentId.map(Chat.tournamentSetup) orElse
        g.simulId.map(Chat.simulSetup)
    }.fold(this)(setup => copy(priv = Right(setup)))
  }

  private[round] case class TakebackSituation(nbDeclined: Int, lastDeclined: Option[DateTime]) {

    def decline = TakebackSituation(nbDeclined + 1, DateTime.now.some)

    def delaySeconds = (math.pow(nbDeclined min 10, 2) * 10).toInt

    def offerable = lastDeclined.fold(true) { _ isBefore DateTime.now.minusSeconds(delaySeconds) }

    def reset = takebackSituationZero.zero
  }

  private[round] implicit val takebackSituationZero: Zero[TakebackSituation] =
    Zero.instance(TakebackSituation(0, none))

  private[round] class Dependencies(
      val messenger: Messenger,
      val takebacker: Takebacker,
      val moretimer: Moretimer,
      val finisher: Finisher,
      val rematcher: Rematcher,
      val player: Player,
      val drawer: Drawer,
      val forecastApi: ForecastApi,
      val isSimulHost: User.ID => Fu[Boolean]
  )
}
