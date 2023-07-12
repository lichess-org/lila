package lila.round

import chess.{ Color, DecayingStats, Status }

import lila.common.{ Bus, Uptime }
import lila.game.actorApi.{ AbortedBy, FinishGame }
import lila.game.{ Game, GameRepo, Pov, RatingDiffs }
import lila.playban.PlaybanApi
import lila.user.{ User, UserRepo, UserApi }
import lila.i18n.{ defaultLang, I18nKeys as trans }

final private class Finisher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    userApi: UserApi,
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    playban: PlaybanApi,
    notifier: RoundNotifier,
    crosstableApi: lila.game.CrosstableApi,
    getSocketStatus: Game => Fu[actorApi.SocketStatus],
    recentTvGames: RecentTvGames
)(using Executor):

  private given play.api.i18n.Lang = defaultLang

  def abort(pov: Pov)(using GameProxy): Fu[Events] =
    apply(pov.game, _.Aborted, None).andDo:
      getSocketStatus(pov.game).foreach: ss =>
        playban.abort(pov, ss.colorsOnGame)
      Bus.publish(AbortedBy(pov.copy(game = pov.game.abort)), "abortGame")

  def abortForce(game: Game)(using GameProxy): Fu[Events] =
    apply(game, _.Aborted, None)

  def rageQuit(game: Game, winner: Option[Color])(using GameProxy): Fu[Events] =
    apply(game, _.Timeout, winner).andDo:
      winner.foreach: color =>
        playban.rageQuit(game, !color)

  def outOfTime(game: Game)(using GameProxy): Fu[Events] =
    if !game.isCorrespondence && !Uptime.startedSinceSeconds(120) && game.movedAt.isBefore(Uptime.startedAt)
    then
      logger.info(s"Aborting game last played before JVM boot: ${game.id}")
      other(game, _.Aborted, none)
    else if game.player(!game.player.color).isOfferingDraw then
      apply(game, _.Draw, None, Messenger.SystemMessage.Persistent(trans.drawOfferAccepted.txt()).some)
    else
      val winner = Some(!game.player.color) ifFalse game.situation.opponentHasInsufficientMaterial
      apply(game, _.Outoftime, winner).andDo:
        winner.foreach: w =>
          playban.flag(game, !w)

  def noStart(game: Game)(using GameProxy): Fu[Events] =
    game.playerWhoDidNotMove.so: culprit =>
      lila.mon.round.expiration.count.increment()
      playban.noStart(Pov(game, culprit))
      if game.isMandatory || game.metadata.hasRule(_.NoAbort) then
        apply(game, _.NoStart, Some(!culprit.color))
      else apply(game, _.Aborted, None, Messenger.SystemMessage.Persistent("Game aborted by server").some)

  def other(
      game: Game,
      status: Status.type => Status,
      winner: Option[Color],
      message: Option[Messenger.SystemMessage] = None
  )(using GameProxy): Fu[Events] =
    apply(game, status, winner, message) andDo playban.other(game, status, winner)

  private def recordLagStats(game: Game) = for
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
  do
    import lila.mon.round.move.{ lag as lRec }
    lRec.mean.record(Math.round(10 * mean))
    lRec.stdDev.record(Math.round(10 * sd))
    // wikipedia.org/wiki/Coefficient_of_variation#Estimation
    lRec.coefVar.record(Math.round((1000f + 250f / moves) * sd / mean))
    lRec.uncomped(quotaStr).record(uncompAvg)
    uncompStats.stdDev foreach { v =>
      lRec.uncompStdDev(quotaStr).record(Math.round(10 * v))
    }
    lt.lagEstimator match
      case h: DecayingStats => lRec.compDeviation.record(h.deviation.toInt)
    lRec.compEstStdErr.record(Math.round(1000 * compEstStdErr))
    lRec.compEstOverErr.record(Math.round(10f * compEstOvers / moves))

  private def apply(
      prev: Game,
      makeStatus: Status.type => Status,
      winnerC: Option[Color],
      message: Option[Messenger.SystemMessage] = None
  )(using proxy: GameProxy): Fu[Events] =
    val status = makeStatus(Status)
    val prog   = lila.game.Progress(prev, prev.finish(status, winnerC))
    val game   = prog.game
    if game.nonAi && game.isCorrespondence then Color.all foreach notifier.gameEnd(prog.game)
    lila.mon.game
      .finish(
        variant = game.variant.key.value,
        source = game.source.fold("unknown")(_.name),
        speed = game.speed.name,
        mode = game.mode.name,
        status = status.name
      )
      .increment()
    recordLagStats(game)
    proxy.save(prog) >>
      gameRepo.finish(
        id = game.id,
        winnerColor = winnerC,
        winnerId = winnerC flatMap (game.player(_).userId),
        status = prog.game.status
      ) >>
      userApi
        .pairWithPerfs(game.userIdPair)
        .flatMap: users =>
          val finish = FinishGame(game, users)
          updateCountAndPerfs(finish).map: ratingDiffs =>
            message.foreach { messenger(game, _) }
            gameRepo game game.id foreach { newGame =>
              newGame foreach proxy.setFinishedGame
              val newFinish = finish.copy(game = newGame | game)
              Bus.publish(newFinish, "finishGame")
              game.userIds.foreach: userId =>
                Bus.publish(newFinish, s"userFinishGame:$userId")
            }
            List(lila.game.Event.EndData(game, ratingDiffs))

  private def updateCountAndPerfs(finish: FinishGame): Fu[Option[RatingDiffs]] =
    (!finish.isVsSelf && !finish.game.aborted).so:
      finish.users.tupled.so { (white, black) =>
        crosstableApi.add(finish.game) zip perfsUpdater.save(finish.game, white, black) dmap (_._2)
      } zip
        (finish.white so incNbGames(finish.game)) zip
        (finish.black so incNbGames(finish.game)) dmap (_._1._1)

  private def incNbGames(game: Game)(user: User.WithPerfs): Funit =
    game.finished so { user.noBot || game.nonAi } so {
      val totalTime = (game.hasClock && user.playTime.isDefined) so game.durationSeconds
      val tvTime    = totalTime ifTrue recentTvGames.get(game.id)
      val result =
        if game.winnerUserId has user.id then 1
        else if game.loserUserId has user.id then -1
        else 0
      userRepo
        .incNbGames(user.id, game.rated, game.hasAi, result = result, totalTime = totalTime, tvTime = tvTime)
        .void
    }
