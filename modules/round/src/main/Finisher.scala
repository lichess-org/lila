package lila.round

import chess.{ ByColor, Color, DecayingStats, Status }
import chess.rating.IntRatingDiff

import lila.common.{ Bus, Uptime }
import lila.core.game.{ AbortedBy, FinishGame }
import lila.core.i18n.{ I18nKey as trans, Translator, defaultLang }
import lila.core.perf.UserWithPerfs
import lila.game.GameExt.finish
import lila.playban.PlaybanApi
import lila.user.{ UserApi, UserRepo }

final private class Finisher(
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    userApi: UserApi,
    messenger: Messenger,
    perfsUpdater: PerfsUpdater,
    playban: PlaybanApi,
    notifier: RoundNotifier,
    crosstableApi: lila.game.CrosstableApi,
    getSocketStatus: Game => Fu[SocketStatus],
    recentTvGames: RecentTvGames
)(using Executor, Translator):

  private given play.api.i18n.Lang = defaultLang

  def abort(pov: Pov)(using GameProxy): Fu[Events] =
    for
      events <- apply(pov.game, _.Aborted, None)
      _ = getSocketStatus(pov.game).foreach: ss =>
        playban.abort(pov, ss.colorsOnGame)
      _ = Bus.pub(AbortedBy(pov.copy(game = pov.game.abort)))
    yield events

  def abortForce(game: Game)(using GameProxy): Fu[Events] =
    apply(game, _.Aborted, None)

  def rageQuit(game: Game, winner: Option[Color])(using GameProxy): Fu[Events] =
    for
      events <- apply(game, _.Timeout, winner)
      _ = winner.foreach: color =>
        playban.rageQuit(game, !color)
    yield events

  def outOfTime(game: Game)(using GameProxy): Fu[Events] =
    if !game.isCorrespondence && !Uptime.startedSinceSeconds(120) && game.movedAt.isBefore(Uptime.startedAt)
    then
      logger.info(s"Aborting game last played before JVM boot: ${game.id}")
      other(game, _.Aborted, none)
    else if game.player(!game.player.color).isOfferingDraw then
      apply(game, _.Draw, None, Messenger.SystemMessage.Persistent(trans.site.drawOfferAccepted.txt()).some)
    else
      val winner = Some(!game.player.color).ifFalse(game.position.opponentHasInsufficientMaterial)
      for
        events <- apply(game, _.Outoftime, winner)
        _ = winner.foreach: w =>
          playban.flag(game, !w)
      yield events

  def noStart(game: Game)(using GameProxy): Fu[Events] =
    game.playerWhoDidNotMove.so: culprit =>
      lila.mon.round.expiration.count.increment()
      playban.noStart(Pov(game, culprit))
      if game.isMandatory || game.metadata.hasRule(_.noAbort) then
        apply(game, _.NoStart, Some(!culprit.color))
      else apply(game, _.Aborted, None, Messenger.SystemMessage.Persistent("Game aborted by server").some)

  def other(
      game: Game,
      status: Status.type => Status,
      winner: Option[Color],
      message: Option[Messenger.SystemMessage] = None
  )(using GameProxy): Fu[Events] =
    for
      events <- apply(game, status, winner, message)
      _ = playban.other(game, status(Status), winner)
    yield events

  private def recordLagStats(game: Game) = for
    clock <- game.clock
    player <- clock.players.all
    lt = player.lag
    stats = lt.lagStats
    moves = lt.moves if moves > 4
    sd <- stats.stdDev
    mean = stats.mean if mean > 0
    uncompAvg = Math.round(10 * lt.uncompStats.mean)
    compEstStdErr <- lt.compEstStdErr
    quotaStr = f"${lt.quotaGain.centis / 10}%02d"
    compEstOvers = lt.compEstOvers.centis
  do
    import lila.mon.round.move.lag as lRec
    lRec.mean.record(Math.round(10 * mean))
    lRec.stdDev.record(Math.round(10 * sd))
    // wikipedia.org/wiki/Coefficient_of_variation#Estimation
    lRec.coefVar.record(Math.round((1000f + 250f / moves) * sd / mean))
    lRec.uncomped(quotaStr).record(uncompAvg)
    lt.uncompStats.stdDev.foreach { v =>
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
    val prog = lila.game.Progress(prev, prev.finish(status, winnerC))
    import prog.game
    if game.nonAi && game.isCorrespondence then Color.all.foreach(notifier.gameEnd(prog.game))
    lila.mon.game
      .finish(game.variant, game.speed, game.source, game.rated, status)
      .increment()
    recordLagStats(game)
    for
      _ <- proxy.save(prog)
      _ <- gameRepo.finish(
        id = game.id,
        winnerColor = winnerC,
        winnerId = winnerC.flatMap(game.player(_).userId),
        status = prog.game.status
      )
      users <- userApi.pairWithPerfs(game.userIdPair)
      ratingDiffs <- updateCountAndPerfs(game, users)
    yield
      message.foreach { messenger(game, _) }
      gameRepo.game(game.id).foreach { newGame =>
        newGame.foreach(proxy.setFinishedGame)
        val finish = FinishGame(newGame | game, users)
        Bus.pub(finish)
        game.userIds.foreach: userId =>
          Bus.publishDyn(finish, s"userFinishGame:$userId")
      }
      List(lila.game.Event.EndData(game, ratingDiffs))

  private def updateCountAndPerfs(
      game: Game,
      users: ByColor[Option[UserWithPerfs]]
  ): Fu[Option[ByColor[IntRatingDiff]]] =
    val isVsSelf = users.tupled.so((w, b) => w._1.is(b._1))
    (!isVsSelf && !game.aborted).so:
      users.tupled
        .map(ByColor.fromPair)
        .so: users =>
          crosstableApi.add(game).zip(perfsUpdater.save(game, users)).dmap(_._2)
        .zip(users.white.so(incNbGames(game)))
        .zip(users.black.so(incNbGames(game)))
        .dmap(_._1._1)

  private def incNbGames(game: Game)(user: UserWithPerfs): Funit =
    (game.finished && (user.noBot || game.nonAi)).so:
      val totalTime = (game.hasClock && user.playTime.isDefined).so(game.durationSeconds)
      val tvTime = totalTime.ifTrue(recentTvGames.get(game.id))
      val result =
        if game.winnerUserId.has(user.id) then 1
        else if game.loserUserId.has(user.id) then -1
        else 0
      userRepo
        .incNbGames(
          user.id,
          game.rated.yes,
          game.hasAi,
          result = result,
          totalTime = totalTime,
          tvTime = tvTime
        )
        .void
