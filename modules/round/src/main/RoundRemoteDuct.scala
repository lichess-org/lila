package lila.round

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import scala.concurrent.duration._

import actorApi._, round._
import chess.{ Color, White, Black, Speed }
import lila.chat.Chat
import lila.game.Game.{ PlayerId, FullId }
import lila.game.{ Game, Progress, Pov, Event, Source, Player => GamePlayer }
import lila.hub.actorApi.DeployPost
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.{ FishnetPlay, FishnetStart, BotPlay, RematchYes, RematchNo, Abort, Resign }
import lila.hub.actorApi.simul.GetHostIds
import lila.hub.actorApi.socket.HasUserId
import lila.hub.Duct
import lila.room.RoomSocket.{ Protocol => RP, _ }
import lila.socket.RemoteSocket.{ Protocol => P, _ }
import lila.socket.Socket.{ Sri, SocketVersion, GetVersion, makeMessage }
import lila.socket.UserLagCache
import lila.user.User
import makeTimeout.large

private[round] final class RoundRemoteDuct(
    dependencies: RoundRemoteDuct.Dependencies,
    gameId: Game.ID,
    socketSend: String => Unit
)(implicit proxy: GameProxy) extends AnyRoundDuct {

  import RoundRemoteSocket.Protocol
  import RoundRemoteDuct._
  import RoundDuct._
  import dependencies._

  private var takebackSituation: Option[TakebackSituation] = None

  private var version = SocketVersion(0)

  private var hasAi = false
  private var mightBeSimul = true // until proven false
  private var gameSpeed: Option[Speed] = none
  private var chatIds = RoundSocket.ChatIds(
    priv = Chat.Id(gameId), // until replaced with tourney/simul chat
    pub = Chat.Id(s"$gameId/w")
  )
  private var tournamentId = none[String] // until set, to listen to standings

  private final class Player(color: Color) {

    private var offlineSince: Option[Long] = nowMillis.some
    // wether the player closed the window intentionally
    private var bye: Boolean = false
    // connected as a bot
    private var botConnected: Boolean = false

    var userId = none[User.ID]
    var goneWeight = 1f

    def isOnline = botConnected || offlineSince.isEmpty

    def setOnline(on: Boolean): Unit = {
      println(s"setOnline $color on=$on offlineSince=$offlineSince isLongGone=$isLongGone")
      isLongGone foreach { _ ?? notifyGone(color, false) }
      offlineSince = if (on) None else offlineSince orElse nowMillis.some
      bye = bye && !on
      println(s"setOnline $color on=$on offlineSince=$offlineSince isLongGone=$isLongGone")
    }
    def setBye: Unit = {
      setOnline(false)
      bye = true
    }

    private def isHostingSimul: Fu[Boolean] = mightBeSimul ?? userId ?? { u =>
      bus.ask[Set[User.ID]]('simulGetHosts)(GetHostIds).map(_ contains u)
    }

    private def timeoutMillis = {
      if (bye) RoundSocket.ragequitTimeout.toMillis else RoundSocket.gameDisconnectTimeout(gameSpeed).toMillis
    } * goneWeight atLeast 12000

    def isLongGone: Fu[Boolean] = fuccess {
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
        playerDo(pov.color, _.setBye)
      }
    }

    case GetVersion(promise) => fuccess {
      promise success version
    }

    case PlayersOnline(white, black) => fuccess {
      whitePlayer setOnline white
      blackPlayer setOnline black
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
      hasAi = game.hasAi
      whitePlayer.userId = game.player(White).userId
      blackPlayer.userId = game.player(Black).userId
      mightBeSimul = game.isSimul
      game.tournamentId orElse game.simulId map Chat.Id.apply foreach { chatId =>
        chatIds = chatIds.copy(priv = chatId)
        buscriptions.chat
      }
      game.tournamentId foreach { tourId =>
        tournamentId = tourId.some
        buscriptions.tournament
      }
      gameSpeed = game.speed.some
      whitePlayer.goneWeight = whiteGoneWeight
      blackPlayer.goneWeight = blackGoneWeight
    }

    case lila.chat.actorApi.ChatLine(chatId, line) => fuccess {
      publish(List(line match {
        case l: lila.chat.UserLine => Event.UserMessage(l, chatId == chatIds.pub)
        case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
      }))
    }

    case Protocol.In.PlayerChatSay(_, Right(color), msg) => fuccess {
      messenger.owner(gameId, color, msg)
    }
    case Protocol.In.PlayerChatSay(_, Left(userId), msg) if chatIds.priv == Chat.Id(gameId) => fuccess {
      messenger.owner(gameId, userId, msg)
    }

    case Protocol.In.HoldAlert(fullId, ip, mean, sd) => handle(fullId.playerId) { pov =>
      lila.game.GameRepo hasHoldAlert pov flatMap {
        case true => funit
        case false =>
          lila.log("cheat").info(s"hold alert $ip https://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
          lila.mon.cheat.holdAlert()
          proxy.persist(_.setHoldAlert(pov, GamePlayer.HoldAlert(ply = pov.game.turns, mean = mean, sd = sd)).void)
      } inject Nil
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
        playerGet(!pov.color, _.isLongGone) flatMap {
          case true if !pov.game.variant.insufficientWinningMaterial(pov.game.board, pov.color) => finisher.rageQuit(pov.game, Some(pov.color))
          case true => finisher.rageQuit(pov.game, None)
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    case DrawForce(playerId) => handle(playerId) { pov =>
      (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock) ?? {
        playerGet(!pov.color, _.isLongGone) flatMap {
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
  }

  private object buscriptions {

    private var classifiers = collection.mutable.Set.empty[Symbol]

    private def sub(classifier: Symbol) = {
      bus.subscribe(RoundRemoteDuct.this, classifier)
      classifiers += classifier
    }

    def unsubAll = {
      bus.unsubscribe(RoundRemoteDuct.this, classifiers.toSeq)
      classifiers.clear
    }

    def subAll = {
      // TODO tv
      chat
      tournament
    }

    // TODO
    // def tv = members.flatMap { case (_, m) => m.userTv }.toSet foreach { (userId: User.ID) =>
    //   sub(Symbol(s"userStartGame:$userId"))
    // }

    def chat = chatIds.all foreach { chatId =>
      sub(lila.chat.Chat classify chatId)
    }

    def tournament = tournamentId foreach { id =>
      sub(Symbol(s"tour-standing-$id"))
    }
  }

  private def playerGet[A](color: Color, getter: Player => A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  private def playerDo(color: Color, effect: Player => Unit): Unit =
    effect(color.fold(whitePlayer, blackPlayer))

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
    _ foreach { pov =>
      socketSend(Protocol.Out.gone(FullId(pov.fullId), gone))
    }
  }

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
        socketSend(RP.Out.tellRoomVersion(roomId, makeMessage(e.typ, e.data), version, e.troll))
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

  buscriptions.subAll
  socketSend(RP.Out.start(roomId))

  override def stop() {
    super.stop()
    buscriptions.unsubAll
    socketSend(RP.Out.stop(roomId))
  }
}

object RoundRemoteDuct {

  case class SetGameInfo(game: lila.game.Game, goneWeights: (Float, Float))

  private[round] case class Dependencies(
      messenger: Messenger,
      takebacker: Takebacker,
      moretimer: Moretimer,
      finisher: Finisher,
      rematcher: Rematcher,
      player: Player,
      drawer: Drawer,
      forecastApi: ForecastApi,
      bus: lila.common.Bus
  )
}
