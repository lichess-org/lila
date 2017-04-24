package lila.round

import akka.actor._
import akka.pattern.ask
import org.joda.time.DateTime
import scala.concurrent.duration._

import actorApi._, round._
import chess.{ Centis, Color }
import lila.game.{ Game, Pov, Event }
import lila.hub.actorApi.DeployPost
import lila.hub.actorApi.map._
import lila.hub.actorApi.round.FishnetPlay
import lila.hub.SequentialActor
import makeTimeout.large

private[round] final class Round(
    gameId: String,
    messenger: Messenger,
    takebacker: Takebacker,
    finisher: Finisher,
    rematcher: Rematcher,
    player: Player,
    drawer: Drawer,
    forecastApi: ForecastApi,
    socketHub: ActorRef,
    moretimeDuration: FiniteDuration,
    activeTtl: Duration
) extends SequentialActor {

  context setReceiveTimeout activeTtl

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'deploy)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  implicit val proxy = new GameProxy(gameId)

  object lags { // player lag in centis
    var white = Centis(0)
    var black = Centis(0)
    def get(c: Color) = c.fold(white, black)
    def set(c: Color, v: Centis) {
      if (c.white) white = v
      else black = v
    }
  }

  var takebackSituation = Round.TakebackSituation()

  def process = {

    case ReceiveTimeout => fuccess {
      self ! SequentialActor.Terminate
    }

    case p: HumanPlay =>
      p.trace.finishFirstSegment()
      handleHumanPlay(p) { pov =>
        if (pov.game outoftime lags.get) finisher.outOfTime(pov.game)
        else {
          lags.set(pov.color, p.lag)
          reportNetworkLag(pov)
          player.human(p, self)(pov)
        }
      } >>- {
        p.trace.finish()
        lila.mon.round.move.full.count()
      }

    case FishnetPlay(uci, currentFen) => handle { game =>
      player.fishnet(game, uci, currentFen, self)
    } >>- lila.mon.round.move.full.count()

    case Abort(playerId) => handle(playerId) { pov =>
      pov.game.abortable ?? finisher.abort(pov)
    }

    case Resign(playerId) => handle(playerId) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case ResignAi => handleAi(proxy.game) { pov =>
      pov.game.resignable ?? finisher.other(pov.game, _.Resign, Some(!pov.color))
    }

    case GoBerserk(color) => handle(color) { pov =>
      pov.game.goBerserk(color) ?? { progress =>
        messenger.system(pov.game, (_.untranslated(
          s"${pov.color.name.capitalize} is going berserk!"
        )))
        proxy.save(progress) >> proxy.invalidating(_ goBerserk pov) inject progress.events
      }
    }

    case ResignForce(playerId) => handle(playerId) { pov =>
      (pov.game.resignable && !pov.game.hasAi && pov.game.hasClock) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true => finisher.rageQuit(pov.game, Some(pov.color))
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    case NoStartColor(color) => handle(color) { pov =>
      finisher.other(pov.game, _.NoStart, Some(!pov.color))
    }

    case DrawForce(playerId) => handle(playerId) { pov =>
      (pov.game.drawable && !pov.game.hasAi && pov.game.hasClock) ?? {
        socketHub ? Ask(pov.gameId, IsGone(!pov.color)) flatMap {
          case true => finisher.rageQuit(pov.game, None)
          case _ => fuccess(List(Event.Reload))
        }
      }
    }

    case Outoftime => handle { game =>
      game.outoftime(lags.get) ?? finisher.outOfTime(game)
    }

    // exceptionally we don't block nor publish events
    // if the game is abandoned, then nobody is around to see it
    // we can also terminate this actor
    case Abandon => fuccess {
      proxy withGame { game =>
        game.abandoned ?? {
          self ! PoisonPill
          if (game.abortable) finisher.other(game, _.Aborted)
          else finisher.other(game, _.Resign, Some(!game.player.color))
        }
      }
    }

    case DrawYes(playerRef) => handle(playerRef)(drawer.yes)
    case DrawNo(playerRef) => handle(playerRef)(drawer.no)
    case DrawClaim(playerId) => handle(playerId)(drawer.claim)
    case DrawForce => handle(drawer force _)
    case Cheat(color) => handle { game =>
      (game.playable && !game.imported) ?? {
        finisher.other(game, _.Cheat, Some(!color))
      }
    }

    case Threefold => proxy withGame { game =>
      drawer autoThreefold game map {
        _ foreach { pov =>
          self ! DrawClaim(pov.player.id)
        }
      }
    }

    case HoldAlert(playerId, mean, sd, ip) => handle(playerId) { pov =>
      !pov.player.hasHoldAlert ?? {
        lila.log("cheat").info(s"hold alert $ip https://lichess.org/${pov.gameId}/${pov.color.name}#${pov.game.turns} ${pov.player.userId | "anon"} mean: $mean SD: $sd")
        lila.mon.cheat.holdAlert()
        proxy.bypass(_.setHoldAlert(pov, mean, sd)) inject List.empty[Event]
      }
    }

    case RematchYes(playerRef) => handle(playerRef)(rematcher.yes)
    case RematchNo(playerRef) => handle(playerRef)(rematcher.no)

    case TakebackYes(playerRef) => handle(playerRef) { pov =>
      takebacker.yes(takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation
          events
      }
    }
    case TakebackNo(playerRef) => handle(playerRef) { pov =>
      takebacker.no(takebackSituation)(pov) map {
        case (events, situation) =>
          takebackSituation = situation
          events
      }
    }

    case Moretime(playerRef) => handle(playerRef) { pov =>
      pov.game.clock.ifTrue(pov.game moretimeable !pov.color) ?? { clock =>
        val newClock = clock.giveTime(!pov.color, moretimeDuration.toCentis)
        val progress = (pov.game withClock newClock) + Event.Clock(newClock)
        messenger.system(pov.game, (_.untranslated(
          "%s + %d seconds".format(!pov.color, moretimeDuration.toSeconds)
        )))
        proxy save progress inject progress.events
      }
    }

    case ForecastPlay(lastMove) => handle { game =>
      forecastApi.nextMove(game, lastMove) map { mOpt =>
        mOpt foreach { move =>
          self ! HumanPlay(game.player.id, move, false, Centis(0))
        }
        Nil
      }
    }

    case DeployPost => handle { game =>
      game.clock.filter(_ => game.playable) ?? { clock =>
        val freeSeconds = 15
        val freeCentis = Centis.ofSeconds(freeSeconds)
        val newClock = clock.giveTime(Color.White, freeCentis).giveTime(Color.Black, freeCentis)
        val progress = (game withClock newClock) + Event.Clock(newClock)
        messenger.system(game, (_.untranslated("Lichess has been updated")))
        messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
        Color.all.foreach { c =>
          messenger.system(game, (_.untranslated(s"$c + $freeSeconds seconds")))
        }
        proxy save progress inject progress.events
      }
    }

    case AbortForMaintenance => handle { game =>
      messenger.system(game, (_.untranslated("Game aborted for server maintenance")))
      messenger.system(game, (_.untranslated("Sorry for the inconvenience!")))
      game.playable ?? finisher.other(game, _.Aborted)
    }

    case AbortForce => handle { game =>
      game.playable ?? finisher.other(game, _.Aborted)
    }
  }

  private def reportNetworkLag(pov: Pov) =
    if (pov.game.turns == 20 || pov.game.turns == 21) List(lags.white, lags.black).foreach { lag =>
      if (lag.centis > 0) lila.mon.round.move.networkLag(lag.centis * 10l)
    }

  private def handle[A](op: Game => Fu[Events]): Funit =
    handleGame(proxy.game)(op)

  private def handle(playerId: String)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy playerPov playerId)(op)

  private def handleHumanPlay(p: HumanPlay)(op: Pov => Fu[Events]): Funit =
    handlePov {
      p.trace.segment("fetch", "db") {
        proxy playerPov p.playerId
      }
    }(op)

  private def handle(color: Color)(op: Pov => Fu[Events]): Funit =
    handlePov(proxy pov color)(op)

  private def handlePov(pov: Fu[Option[Pov]])(op: Pov => Fu[Events]): Funit = publish {
    pov flatten "pov not found" flatMap { p =>
      if (p.player.isAi) fufail(s"player $p can't play AI") else op(p)
    }
  } recover errorHandler("handlePov")

  private def handleAi(game: Fu[Option[Game]])(op: Pov => Fu[Events]): Funit = publish {
    game.map(_.flatMap(_.aiPov)) flatten "pov not found" flatMap op
  } recover errorHandler("handleAi")

  private def handleGame(game: Fu[Option[Game]])(op: Game => Fu[Events]): Funit = publish {
    game flatten "game not found" flatMap op
  } recover errorHandler("handleGame")

  private def publish[A](op: Fu[Events]): Funit = op.map { events =>
    if (events.nonEmpty) socketHub ! Tell(gameId, EventList(events))
    if (events exists {
      case e: Event.Move => e.threefold
      case _ => false
    }) self ! Threefold
  }

  private def errorHandler(name: String): PartialFunction[Throwable, Unit] = {
    case e: ClientError =>
      logger.info(s"Round client error $name", e)
      lila.mon.round.error.client()
    case e: FishnetError =>
      logger.info(s"Round fishnet error $name", e)
      lila.mon.round.error.fishnet()
    case e: Exception => logger.warn(s"$name: ${e.getMessage}")
  }
}

object Round {

  case class TakebackSituation(
      nbDeclined: Int = 0,
      lastDeclined: Option[DateTime] = none
  ) {

    def decline = TakebackSituation(nbDeclined + 1, DateTime.now.some)

    def delaySeconds = (math.pow(nbDeclined min 10, 2) * 10).toInt

    def offerable = lastDeclined.fold(true) { _ isBefore DateTime.now.minusSeconds(delaySeconds) }

    def reset = TakebackSituation()
  }

}
