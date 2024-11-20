package lila.round

import monocle.syntax.all.*
import chess.{ ByColor, Color, Speed, glicko }
import chess.glicko.{ Glicko, IntRating, IntRatingDiff, RatingProvisional }

import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.rating.PerfExt.addOrReset
import lila.rating.{ PerfType, RatingFactor, RatingRegulator }
import lila.user.{ RankingApi, UserApi }
import monocle.syntax.AppliedPLens
import lila.rating.PerfExt.toGlickoPlayer

final class PerfsUpdater(
    gameRepo: lila.game.GameRepo,
    userApi: UserApi,
    rankingApi: RankingApi,
    farming: FarmBoostDetection,
    ratingFactors: () => RatingFactor.ByKey
)(using Executor):

  def save(game: Game, users: ByColor[UserWithPerfs]): Fu[Option[ByColor[IntRatingDiff]]] =
    farming.botFarming(game).flatMap {
      if _ then fuccess(none)
      else if farming.newAccountBoosting(game, users) then fuccess(none)
      else
        val ratingDiffs = for
          outcome <- game.outcome
          perfKey <-
            if game.variant.fromPosition
            then game.isTournament.option(PerfKey(game.ratingVariant, game.speed))
            else game.perfKey.some
          if game.rated && game.finished && (game.playedTurns >= 2 || game.isTournament)
          if !users.exists(_.user.lame)
        yield
          val prevPerfs   = users.map(_.perfs)
          val prevPlayers = prevPerfs.map(_(perfKey).toGlickoPlayer)
          lila.rating.Glicko.calculator
            .computeGame(glicko.Game(prevPlayers, outcome))
            .fold(
              err =>
                lila.log("rating").error(s"Error computing Glicko2 for game ${game.id}", err)
                fuccess(none)
              ,
              computedPlayers =>
                val newGlickos = RatingRegulator(ratingFactors())(
                  perfKey,
                  prevPlayers.map(_.glicko),
                  computedPlayers.map(_.glicko),
                  users.map(_.isBot)
                )
                val newPerfs =
                  prevPerfs.zip(newGlickos, (perfs, gl) => addToPerfs(game, perfs, perfKey, gl))
                val ratingDiffs =
                  def ratingOf(perfs: UserPerfs) = perfs(perfKey).glicko.intRating.value
                  prevPerfs.zip(newPerfs, (prev, next) => IntRatingDiff(ratingOf(next) - ratingOf(prev)))
                val newUsers = users.zip(newPerfs, (user, perfs) => user.copy(perfs = perfs))
                lila.common.Bus.publish(lila.core.game.PerfsUpdate(game, newUsers), "perfsUpdate")
                gameRepo
                  .setRatingDiffs(game.id, ratingDiffs)
                  .zip(userApi.updatePerfs(prevPerfs.zip(newPerfs), perfKey))
                  .zip(rankingApi.save(users.white.user, perfKey, newPerfs.white))
                  .zip(rankingApi.save(users.black.user, perfKey, newPerfs.black))
                  .inject(ratingDiffs.some)
            )
        ~ratingDiffs
    }

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
