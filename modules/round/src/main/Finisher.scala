package lila.round

import chess.{ Color, DecayingStats, Status }

import lila.common.{ Bus, Uptime }
import lila.game.actorApi.{ AbortedBy, FinishGame }
import lila.game.{ Game, GameRepo, Pov, RatingDiffs }
import lila.playban.PlaybanApi
import lila.user.{ User, UserRepo }
import lila.i18n.{ I18nKeys => trans, defaultLang }

final private class Finisher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    playban: PlaybanApi,
    notifier: RoundNotifier,
    crosstableApi: lila.game.CrosstableApi,
    getSocketStatus: Game => Fu[actorApi.SocketStatus],
    recentTvGames: RecentTvGames
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  def abort(pov: Pov)(implicit proxy: GameProxy): Fu[Events] =
    apply(pov.game, _.Aborted, None) >>- {
      getSocketStatus(pov.game) foreach { ss =>
        playban.abort(pov, ss.colorsOnGame)
      }
      Bus.publish(AbortedBy(pov.copy(game = pov.game.abort)), "abortGame")
    }

  def abortForce(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    apply(game, _.Aborted, None)

  def rageQuit(game: Game, winner: Option[Color])(implicit proxy: GameProxy): Fu[Events] =
    apply(game, _.Timeout, winner) >>-
      winner.foreach { color =>
        playban.rageQuit(game, !color)
      }

  def outOfTime(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    if (
      !game.isCorrespondence && !Uptime.startedSinceSeconds(120) && game.movedAt.isBefore(Uptime.startedAt)
    ) {
      logger.info(s"Aborting game last played before JVM boot: ${game.id}")
      other(game, _.Aborted, none)

    } else if (game.player(!game.player.color).isOfferingDraw) {
      apply(game, _.Draw, None, Some(trans.drawOfferAccepted.txt()))
    } else {
      val winner = Some(!game.player.color) ifFalse game.situation.opponentHasInsufficientMaterial
      apply(game, _.Outoftime, winner) >>-
        winner.foreach { w =>
          playban.flag(game, !w)
        }
    }

  def noStart(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    game.playerWhoDidNotMove ?? { culprit =>
      lila.mon.round.expiration.count.increment()
      playban.noStart(Pov(game, culprit))
      if (game.isMandatory) apply(game, _.NoStart, Some(!culprit.color))
      else apply(game, _.Aborted, None, Some("Game aborted by server"))
    }

  def other(
      game: Game,
      status: Status.type => Status,
      winner: Option[Color],
      message: Option[String] = None
  )(implicit proxy: GameProxy): Fu[Events] =
    apply(game, status, winner, message) >>- playban.other(game, status, winner).unit

  private def recordLagStats(game: Game): Unit =
    for {
      clock  <- game.clock
      player <- clock.players.all
      lt    = player.lag
      stats = lt.lagStats
      moves = lt.moves if moves > 4
      sd <- stats.stdDev
      mean        = stats.mean if mean > 0
      uncompStats = lt.uncompStats
      uncompAvg   = Math.round(10 * uncompStats.mean)
      compEstStdErr <- lt.compEstStdErr
      quotaStr     = f"${lt.quotaGain.centis / 10}%02d"
      compEstOvers = lt.compEstOvers.centis
    } {
      import lila.mon.round.move.{ lag => lRec }
      lRec.mean.record(Math.round(10 * mean))
      lRec.stdDev.record(Math.round(10 * sd))
      // wikipedia.org/wiki/Coefficient_of_variation#Estimation
      lRec.coefVar.record(Math.round((1000f + 250f / moves) * sd / mean))
      lRec.uncomped(quotaStr).record(uncompAvg)
      uncompStats.stdDev foreach { v =>
        lRec.uncompStdDev(quotaStr).record(Math.round(10 * v))
      }
      lt.lagEstimator match {
        case h: DecayingStats => lRec.compDeviation.record(h.deviation.toInt)
      }
      lRec.compEstStdErr.record(Math.round(1000 * compEstStdErr))
      lRec.compEstOverErr.record(Math.round(10f * compEstOvers / moves))
    }

  private def apply(
      game: Game,
      makeStatus: Status.type => Status,
      winnerC: Option[Color],
      message: Option[String] = None
  )(implicit proxy: GameProxy): Fu[Events] = {
    val status = makeStatus(Status)
    val prog   = game.finish(status, winnerC)
    if (game.nonAi && game.isCorrespondence) Color.all foreach notifier.gameEnd(prog.game)
    lila.mon.game
      .finish(
        variant = game.variant.key,
        source = game.source.fold("unknown")(_.name),
        speed = game.speed.name,
        mode = game.mode.name,
        status = status.name
      )
      .increment()
    val g = prog.game
    recordLagStats(g)
    proxy.save(prog) >>
      gameRepo.finish(
        id = g.id,
        winnerColor = winnerC,
        winnerId = winnerC flatMap (g.player(_).userId),
        status = prog.game.status
      ) >>
      userRepo
        .pair(
          g.whitePlayer.userId,
          g.blackPlayer.userId
        )
        .flatMap { case (whiteO, blackO) =>
          val finish = FinishGame(g, whiteO, blackO)
          updateCountAndPerfs(finish) map { ratingDiffs =>
            message foreach { messenger.system(g, _) }
            gameRepo game g.id foreach { newGame =>
              newGame foreach proxy.setFinishedGame
              val newFinish = finish.copy(game = newGame | g)
              Bus.publish(newFinish, "finishGame")
              game.userIds foreach { userId =>
                Bus.publish(newFinish, s"userFinishGame:$userId")
              }
            }
            prog.events :+ lila.game.Event.EndData(g, ratingDiffs)
          }
        }
  }

  private def updateCountAndPerfs(finish: FinishGame): Fu[Option[RatingDiffs]] =
    (!finish.isVsSelf && !finish.game.aborted) ?? {
      import cats.implicits._
      (finish.white, finish.black).mapN((_, _)) ?? { case (white, black) =>
        crosstableApi.add(finish.game) zip perfsUpdater.save(finish.game, white, black) dmap (_._2)
      } zip
        (finish.white ?? incNbGames(finish.game)) zip
        (finish.black ?? incNbGames(finish.game)) dmap (_._1._1)
    }

  private def incNbGames(game: Game)(user: User): Funit =
    game.finished ?? {
      val totalTime = (game.hasClock && user.playTime.isDefined) ?? game.durationSeconds
      val tvTime    = totalTime ifTrue recentTvGames.get(game.id)
      val result =
        if (game.winnerUserId has user.id) 1
        else if (game.loserUserId has user.id) -1
        else 0
      userRepo
        .incNbGames(user.id, game.rated, game.hasAi, result = result, totalTime = totalTime, tvTime = tvTime)
        .void
    }
}
