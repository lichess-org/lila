package lila.round

import chess.{ ByColor, Color, Speed, IntRating }
import chess.rating.{ IntRatingDiff, RatingProvisional }
import chess.rating.glicko.{ Glicko, Player }

import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.rating.PerfExt.addOrReset
import lila.rating.{ PerfType, RatingFactor, RatingRegulator }
import lila.user.{ RankingApi, UserApi }
import lila.rating.PerfExt.toGlickoPlayer

final class PerfsUpdater(
    gameRepo: lila.game.GameRepo,
    userApi: UserApi,
    rankingApi: RankingApi,
    farming: FarmBoostDetection,
    ratingFactors: () => RatingFactor.ByKey
)(using Executor):

  def save(game: Game, users: ByColor[UserWithPerfs]): Fu[Option[ByColor[IntRatingDiff]]] =
    farming
      .botFarming(game)
      .flatMap:
        if _ then fuccess(none)
        else if farming.newAccountBoosting(game, users) then fuccess(none)
        else calculateRatingAndPerfs(game, users).fold(fuccess(none))(saveRatings(game.id, users))

  private def calculateRatingAndPerfs(game: Game, users: ByColor[UserWithPerfs]): Option[
    (ByColor[IntRatingDiff], ByColor[UserWithPerfs], PerfKey)
  ] =
    for
      outcome <- game.outcome
      perfKey <-
        if game.variant.fromPosition
        then game.isTournament.option(PerfKey(game.ratingVariant, game.speed))
        else game.perfKey.some
      if game.rated && game.finished && (game.playedTurns >= 2 || game.isTournament)
      if !users.exists(_.user.lame)
      prevPerfs   = users.map(_.perfs)
      prevPlayers = prevPerfs.map(_(perfKey).toGlickoPlayer)
      computedPlayers <- computeGlicko(game.id, prevPlayers, outcome)
    yield
      val newGlickos = RatingRegulator(ratingFactors())(
        perfKey,
        prevPlayers.map(_.glicko),
        computedPlayers.map(_.glicko),
        users.map(_.isBot)
      )
      val newPerfs = prevPerfs.zip(newGlickos, (perfs, gl) => addToPerfs(game, perfs, perfKey, gl))
      val ratingDiffs =
        def ratingOf(perfs: UserPerfs) = perfs(perfKey).glicko.intRating.value
        prevPerfs.zip(newPerfs, (prev, next) => IntRatingDiff(ratingOf(next) - ratingOf(prev)))
      val newUsers = users.zip(newPerfs, (user, perfs) => user.copy(perfs = perfs))
      lila.common.Bus.publish(lila.core.game.PerfsUpdate(game, newUsers), "perfsUpdate")
      (ratingDiffs, newUsers, perfKey)

  private def computeGlicko(gameId: GameId, prevPlayers: ByColor[Player], outcome: chess.Outcome) =
    lila.rating.Glicko.calculator
      .computeGame(chess.rating.glicko.Game(prevPlayers, outcome), skipDeviationIncrease = true)
      .onError: err =>
        scala.util.Success(lila.log("rating").warn(s"Error computing Glicko2 for game $gameId", err))
      .toOption

  private def saveRatings(gameId: GameId, prevUsers: ByColor[UserWithPerfs])(
      ratingDiffs: ByColor[IntRatingDiff],
      newUsers: ByColor[UserWithPerfs],
      perfKey: PerfKey
  ): Fu[Option[ByColor[IntRatingDiff]]] =
    gameRepo
      .setRatingDiffs(gameId, ratingDiffs)
      .zip(userApi.updatePerfs(prevUsers.map(_.perfs).zip(newUsers.map(_.perfs)), perfKey))
      .zip(rankingApi.save(newUsers, perfKey))
      .inject(ratingDiffs.some)

  private def addToPerfs(game: Game, perfs: UserPerfs, perfKey: PerfKey, player: Glicko) =
    val newPerfs = perfs
      .focusKey(perfKey)
      .modify:
        _.addOrReset(_.round.error.glicko, s"game ${game.id}")(player, game.movedAt)
    if game.ratingVariant.standard
    then updateStandard(newPerfs)
    else newPerfs

  private def updateStandard(p: UserPerfs) =
    p.copy(
      standard =
        val subs = List(p.bullet, p.blitz, p.rapid, p.classical, p.correspondence).filter(_.provisional.no)
        subs.maxByOption(_.latest.fold(0L)(_.toMillis)).flatMap(_.latest).fold(p.standard) { date =>
          val nb = subs.map(_.nb).sum
          val glicko = Glicko(
            rating = subs.map(s => s.glicko.rating * (s.nb / nb.toDouble)).sum,
            deviation = subs.map(s => s.glicko.deviation * (s.nb / nb.toDouble)).sum,
            volatility = subs.map(s => s.glicko.volatility * (s.nb / nb.toDouble)).sum
          )
          Perf(
            glicko = glicko,
            nb = nb,
            recent = Nil,
            latest = date.some
          )
        }
    )
