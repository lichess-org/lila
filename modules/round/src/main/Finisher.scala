package lila.round

import chess.{ Status, DecayingStats, Color, Clock }

import lila.game.actorApi.{ FinishGame, AbortedBy }
import lila.game.{ GameRepo, Game, Pov, RatingDiffs }
import lila.i18n.I18nKey.{ Select => SelectI18nKey }
import lila.playban.PlaybanApi
import lila.user.{ User, UserRepo }

private[round] final class Finisher(
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    playban: PlaybanApi,
    notifier: RoundNotifier,
    crosstableApi: lila.game.CrosstableApi,
    bus: lila.common.Bus,
    getSocketStatus: Game.ID => Fu[actorApi.SocketStatus],
    isRecentTv: Game.ID => Boolean
) {

  def abort(pov: Pov)(implicit proxy: GameProxy): Fu[Events] = apply(pov.game, _.Aborted, None) >>- {
    getSocketStatus(pov.gameId) foreach { ss =>
      playban.abort(pov, ss.colorsOnGame)
    }
    bus.publish(AbortedBy(pov), 'abortGame)
  }

  def rageQuit(game: Game, winner: Option[Color])(implicit proxy: GameProxy): Fu[Events] =
    apply(game, _.Timeout, winner) >>-
      winner.?? { color => playban.rageQuit(game, !color) }

  def outOfTime(game: Game)(implicit proxy: GameProxy): Fu[Events] = {
    import lila.common.PlayApp
    if (!PlayApp.startedSinceSeconds(60) && (game.movedAt isBefore PlayApp.startedAt)) {
      logger.info(s"Aborting game last played before JVM boot: ${game.id}")
      other(game, _.Aborted, none)
    } else {
      val winner = Some(!game.player.color) filterNot { color =>
        game.variant.insufficientWinningMaterial(game.board, color)
      }
      apply(game, _.Outoftime, winner) >>-
        winner.?? { w => playban.flag(game, !w) }
    }
  }

  def noStart(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    game.playerWhoDidNotMove ?? { culprit =>
      lila.mon.round.expiration.count()
      playban.noStart(Pov(game, culprit))
      if (game.isMandatory) apply(game, _.NoStart, Some(!culprit.color))
      else apply(game, _.Aborted, None, Some(_.untranslated("Game aborted by server")))
    }

  def other(
    game: Game,
    status: Status.type => Status,
    winner: Option[Color],
    message: Option[SelectI18nKey] = None
  )(implicit proxy: GameProxy): Fu[Events] =
    apply(game, status, winner, message) >>- playban.other(game, status, winner)

  private def recordLagStats(game: Game): Unit = for {
    clock <- game.clock
    player <- clock.players.all
    lt = player.lag
    stats = lt.lagStats
    moves = lt.moves if moves > 4
    sd <- stats.stdDev
    mean = stats.mean if mean > 0
    uncompStats = lt.uncompStats
    uncompAvg = Math.round(10 * uncompStats.mean)
    compEstStdErr <- lt.compEstStdErr
    quotaStr = f"${lt.quotaGain.centis / 10}%02d"
    compEstOvers = lt.compEstOvers.centis
  } {
    import lila.mon.round.move.{ lag => lRec }
    lRec.mean(Math.round(10 * mean))
    lRec.stdDev(Math.round(10 * sd))
    // wikipedia.org/wiki/Coefficient_of_variation#Estimation
    lRec.coefVar(Math.round((1000f + 250f / moves) * sd / mean))
    lRec.uncomped(quotaStr)(uncompAvg)
    lRec.uncompedAll(uncompAvg)
    uncompStats.stdDev foreach { v =>
      lRec.uncompStdDev(quotaStr)(Math.round(10 * v))
    }
    lt.lagEstimator match {
      case h: DecayingStats => lRec.compDeviation(h.deviation.toInt)
    }
    lRec.compEstStdErr(Math.round(1000 * compEstStdErr))
    lRec.compEstOverErr(Math.round(10f * compEstOvers / moves))
  }

  private def apply(
    game: Game,
    makeStatus: Status.type => Status,
    winner: Option[Color] = None,
    message: Option[SelectI18nKey] = None
  )(implicit proxy: GameProxy): Fu[Events] = {
    val status = makeStatus(Status)
    val prog = game.finish(status, winner)
    if (game.nonAi && game.isCorrespondence) Color.all foreach notifier.gameEnd(prog.game)
    lila.mon.game.finish(status.name)()
    val g = prog.game
    recordLagStats(g)
    proxy.save(prog) >>
      GameRepo.finish(
        id = g.id,
        winnerColor = winner,
        winnerId = winner flatMap (g.player(_).userId),
        status = prog.game.status
      ) >>
      UserRepo.pair(
        g.whitePlayer.userId,
        g.blackPlayer.userId
      ).flatMap {
          case (whiteO, blackO) => {
            val finish = FinishGame(g, whiteO, blackO)
            updateCountAndPerfs(finish) map { ratingDiffs =>
              message foreach { messenger.system(g, _) }
              GameRepo game g.id foreach { newGame =>
                bus.publish(finish.copy(game = newGame | g), 'finishGame)
              }
              prog.events :+ lila.game.Event.EndData(g, ratingDiffs)
            }
          }
        }
  } >>- proxy.invalidate

  private def updateCountAndPerfs(finish: FinishGame): Fu[Option[RatingDiffs]] =
    (!finish.isVsSelf && !finish.game.aborted) ?? {
      (finish.white |@| finish.black).tupled ?? {
        case (white, black) =>
          crosstableApi.add(finish.game) zip perfsUpdater.save(finish.game, white, black) map {
            case _ ~ ratingDiffs => ratingDiffs
          }
      } zip
        (finish.white ?? incNbGames(finish.game)) zip
        (finish.black ?? incNbGames(finish.game)) map {
          case ratingDiffs ~ _ ~ _ => ratingDiffs
        }
    }

  private def incNbGames(game: Game)(user: User): Funit = game.finished ?? {
    val totalTime = (game.hasClock && user.playTime.isDefined) ?? game.durationSeconds
    val tvTime = totalTime ifTrue isRecentTv(game.id)
    val result =
      if (game.winnerUserId has user.id) 1
      else if (game.loserUserId has user.id) -1
      else 0
    UserRepo.incNbGames(user.id, game.rated, game.hasAi,
      result = result,
      totalTime = totalTime,
      tvTime = tvTime).void
  }
}
