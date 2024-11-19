package lila.round

import chess.{ ByColor, Color, Speed, glicko }
import monocle.syntax.all.*

import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.rating.GlickoExt.average
import lila.rating.PerfExt.{ addOrReset, toRating }
import lila.rating.{ Glicko, PerfType, RatingFactors, RatingRegulator, glicko2 }
import lila.user.{ RankingApi, UserApi }
import monocle.syntax.AppliedPLens
import lila.rating.PerfExt.toGlickoPlayer

final class PerfsUpdater(
    gameRepo: lila.game.GameRepo,
    userApi: UserApi,
    rankingApi: RankingApi,
    farming: FarmBoostDetection,
    ratingFactors: () => RatingFactors
)(using Executor):

  TODO.use(ratingFactors)
  def save(game: Game, users: ByColor[UserWithPerfs]): Fu[Option[ByColor[IntRatingDiff]]] =
    farming.botFarming(game).flatMap {
      if _ then fuccess(none)
      else if farming.newAccountBoosting(game, users) then fuccess(none)
      else
        val ratingPerf: Option[PerfKey] =
          if game.variant.fromPosition
          then game.isTournament.option(PerfKey(game.ratingVariant, game.speed))
          else game.perfKey.some
        ratingPerf.so: perfKey =>
          val canBeRated =
            game.rated &&
              game.finished &&
              (game.playedTurns >= 2 || game.isTournament)
              && !users.exists(_.user.lame)
          canBeRated.so:
            val prevPerfs     = users.map(_.perfs)
            val glickoPlayers = prevPerfs.map(_(perfKey).toGlickoPlayer)
            val newPerfs =
              prevPerfs.zip(glickoPlayers, (perfs, player) => addToPerfs(game, perfs, perfKey, player))
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
    }

  private def addToPerfs(game: Game, perfs: UserPerfs, perfKey: PerfKey, player: glicko.Player) =
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
          val glicko = new lila.core.rating.Glicko(
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
