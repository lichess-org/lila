package lila.evaluation

import chess.{ Color, Speed }
import lila.analyse.{ Accuracy, Analysis }
import lila.game.{ Game, Pov }
import org.joda.time.DateTime

case class Analysed(game: Game, analysis: Analysis)

case class Assessible(analysed: Analysed, color: Color) {
  import Statistics._
  import analysed._

  def suspiciousErrorRate: Boolean =
    listAverage(Accuracy.diffsList(Pov(game, color), analysis)) < (game.speed match {
      case Speed.Bullet => 25
      case Speed.Blitz => 20
      case _ => 15
    })

  def alwaysHasAdvantage: Boolean =
    !analysis.infos.exists { info =>
      info.cp.fold(info.mate.fold(false) { a => (a.signum == color.fold(-1, 1)) }) { cp =>
        color.fold(cp.centipawns < -100, cp.centipawns > 100)
      }
    }

  def highBlurRate: Boolean =
    !game.isSimul && game.playerBlurPercent(color) > 90

  def moderateBlurRate: Boolean =
    !game.isSimul && game.playerBlurPercent(color) > 70

  def suspiciousHoldAlert: Boolean =
    game.player(color).hasSuspiciousHoldAlert

  def highestChunkBlurs: Float =
    game.player(color).blurs match {
      case bits: lila.game.Blurs.Bits => bits.booleans.iterator.sliding(10).map(_.count(true==)).max
      case _ => 0
    }

  def highChunkBlurRate: Boolean =
    highestChunkBlurs >= 9

  def moderateChunkBlurRate: Boolean =
    highestChunkBlurs >= 7

  def highlyConsistentMoveTimes: Boolean =
    if (game.clock.forall(_.estimateTotalSeconds > 60))
      moveTimeCoefVariation(Pov(game, color)) ?? { cvIndicatesHighlyFlatTimes(_) }
    else
      false

  // moderatelyConsistentMoveTimes must stay in Statistics because it's used in classes that do not use Assessible

  def highlyConsistentMoveTimeStreaks: Boolean =
    if (game.clock.forall(_.estimateTotalSeconds > 60))
      slidingMoveTimesCvs(Pov(game, color)) ?? {
        _ exists cvIndicatesHighlyFlatTimesForStreaks
      }
    else
      false

  def moderatelyConsistentMoveTimeStreaks: Boolean =
    slidingMoveTimesCvs(Pov(game, color)) ?? {
      _ exists cvIndicatesModeratelyFlatTimes
    }

  def mkFlags: PlayerFlags = PlayerFlags(
    suspiciousErrorRate,
    alwaysHasAdvantage,
    highBlurRate || highChunkBlurRate,
    moderateBlurRate || moderateChunkBlurRate,
    highlyConsistentMoveTimes || highlyConsistentMoveTimeStreaks,
    moderatelyConsistentMoveTimes(Pov(game, color)) || moderatelyConsistentMoveTimeStreaks,
    noFastMoves(Pov(game, color)),
    suspiciousHoldAlert
  )

  private val T = true
  private val F = false

  private def rankCheating: GameAssessment = {
    import GameAssessment._
    val flags = mkFlags
    val assessment = flags match {
      //               SF1 SF2 BLR1 BLR2 HCMT MCMT NFM Holds
      case PlayerFlags(T, T, T, T, T, T, T, T) => Cheating // all T, obvious cheat
      case PlayerFlags(T, _, T, _, _, _, T, _) => Cheating // high accuracy, high blurs, no fast moves
      case PlayerFlags(T, _, _, T, _, _, _, _) => Cheating // high accuracy, moderate blurs
      case PlayerFlags(T, _, _, _, T, _, _, _) => Cheating // high accuracy, highly consistent move times
      case PlayerFlags(_, _, T, _, T, _, _, _) => Cheating // high blurs, highly consistent move times

      case PlayerFlags(_, _, _, T, _, T, _, _) => LikelyCheating // moderate blurs, consistent move times
      case PlayerFlags(T, _, _, _, _, _, _, T) => LikelyCheating // Holds are bad, hmk?
      case PlayerFlags(_, T, _, _, _, _, _, T) => LikelyCheating // Holds are bad, hmk?
      case PlayerFlags(_, _, _, _, T, _, _, _) => LikelyCheating // very consistent move times
      case PlayerFlags(_, T, T, _, _, _, _, _) => LikelyCheating // always has advantage, high blurs

      case PlayerFlags(_, T, _, _, _, T, T, _) => Unclear // always has advantage, consistent move times
      case PlayerFlags(T, _, _, _, _, T, T, _) => Unclear // high accuracy, consistent move times, no fast moves
      case PlayerFlags(T, _, _, F, _, F, T, _) => Unclear // high accuracy, no fast moves, but doesn't blur or flat line

      case PlayerFlags(T, _, _, _, _, _, F, _) => UnlikelyCheating // high accuracy, but has fast moves

      case PlayerFlags(F, F, _, _, _, _, _, _) => NotCheating // low accuracy, doesn't hold advantage
      case _ => NotCheating
    }

    if (flags.suspiciousHoldAlert) assessment
    else if (~game.wonBy(color)) assessment
    else if (assessment == Cheating || assessment == LikelyCheating) Unclear
    else assessment
  }

  def sfAvg: Int = listAverage(Accuracy.diffsList(Pov(game, color), analysis)).toInt
  def sfSd: Int = listDeviation(Accuracy.diffsList(Pov(game, color), analysis)).toInt
  def mtAvg: Int = listAverage(~game.moveTimes(color) map (_.roundTenths)).toInt
  def mtSd: Int = listDeviation(~game.moveTimes(color) map (_.roundTenths)).toInt
  def blurs: Int = game.playerBlurPercent(color)
  def hold: Boolean = game.player(color).hasSuspiciousHoldAlert

  def playerAssessment: PlayerAssessment =
    PlayerAssessment(
      _id = game.id + "/" + color.name,
      gameId = game.id,
      userId = ~game.player(color).userId,
      white = (color == Color.White),
      assessment = rankCheating,
      date = DateTime.now,
      // meta
      flags = mkFlags,
      sfAvg = sfAvg,
      sfSd = sfSd,
      mtAvg = mtAvg,
      mtSd = mtSd,
      blurs = blurs,
      hold = hold
    )
}
