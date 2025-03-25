package lila.evaluation

import chess.{ Color, Speed }

import lila.analyse.{ AccuracyCP, Analysis }
import lila.game.GameExt.playerBlurPercent
import lila.game.Player.HoldAlert

case class PlayerAssessment(
    _id: String,
    gameId: GameId,
    userId: UserId,
    color: Color,
    assessment: GameAssessment,
    date: Instant,
    basics: PlayerAssessment.Basics,
    analysis: Statistics.IntAvgSd,
    flags: PlayerFlags,
    tcFactor: Option[Double]
)

object PlayerAssessment:

  val minPlies = 36

  // when you don't have computer analysis
  case class Basics(
      moveTimes: Statistics.IntAvgSd,
      hold: Boolean,
      blurs: Int,
      blurStreak: Option[Int],
      mtStreak: Boolean
  )

  private def highestChunkBlursOf(pov: Pov) =
    import lila.game.Blurs.booleans
    pov.player.blurs.booleans.sliding(12).map(_.count(identity)).max

  private def highlyConsistentMoveTimeStreaksOf(pov: Pov): Boolean =
    pov.game.clock.exists(_.estimateTotalSeconds > 60) && {
      Statistics
        .slidingMoveTimesCvs(pov)
        .so:
          _.exists(Statistics.cvIndicatesHighlyFlatTimesForStreaks)
    }

  private def antichessCorrectedGameAssessment(
      assessment: GameAssessment,
      pov: Pov,
      flags: PlayerFlags
  ): GameAssessment =
    import pov.{ color, game }
    if game.variant != chess.variant.Antichess ||
      (assessment != GameAssessment.Cheating && assessment != GameAssessment.LikelyCheating)
    then assessment
    else if flags.highlyConsistentMoveTimes && flags.suspiciousErrorRate && game.playedTurns < 50 && game
        .sansOf(!color)
        .takeRight(12)
        .count(_.value.startsWith("B")) > 6
    then GameAssessment.Unclear
    else assessment

  def makeBasics(pov: Pov, holdAlerts: Option[HoldAlert]): PlayerAssessment.Basics =
    import Statistics.*
    import pov.{ color, game }
    import lila.game.GameExt.*

    Basics(
      moveTimes = intAvgSd(lila.game.GameExt.computeMoveTimes(game, color).orZero.map(_.roundTenths)),
      blurs = game.playerBlurPercent(color),
      hold = holdAlerts.exists(_.suspicious),
      blurStreak = highestChunkBlursOf(pov).some.filter(0 <),
      mtStreak = highlyConsistentMoveTimeStreaksOf(pov)
    )

  def make(pov: Pov, analysis: Analysis, holdAlerts: Option[HoldAlert]): PlayerAssessment =
    import Statistics.*
    import pov.{ color, game }

    val basics = makeBasics(pov, holdAlerts)

    def blursMatter = !game.isSimul && game.hasClock

    lazy val highBlurRate: Boolean =
      blursMatter && game.playerBlurPercent(color) > 90

    lazy val moderateBlurRate: Boolean =
      blursMatter && game.playerBlurPercent(color) > 70

    val highestChunkBlurs = highestChunkBlursOf(pov)

    val highChunkBlurRate: Boolean = blursMatter && highestChunkBlurs >= 11

    val moderateChunkBlurRate: Boolean = blursMatter && highestChunkBlurs >= 8

    lazy val highlyConsistentMoveTimes: Boolean =
      game.clock.exists(_.estimateTotalSeconds > 60) && {
        moveTimeCoefVariation(pov).so(cvIndicatesHighlyFlatTimes)
      }

    lazy val suspiciousErrorRate: Boolean =
      listAverage(AccuracyCP.diffsList(pov.sideAndStart, analysis).flatten) < (game.speed match
        case Speed.Bullet => 25
        case Speed.Blitz  => 20
        case _            => 15)

    lazy val alwaysHasAdvantage: Boolean =
      !analysis.infos.exists { info =>
        info.cp.fold(info.mate.fold(false) { a =>
          a.signum == color.fold(-1, 1)
        }) { cp =>
          color.fold(cp.centipawns < -100, cp.centipawns > 100)
        }
      }

    lazy val flags: PlayerFlags = PlayerFlags(
      suspiciousErrorRate,
      alwaysHasAdvantage,
      highBlurRate || highChunkBlurRate,
      moderateBlurRate || moderateChunkBlurRate,
      highlyConsistentMoveTimes || highlyConsistentMoveTimeStreaksOf(pov),
      moderatelyConsistentMoveTimes(pov),
      noFastMoves(pov),
      basics.hold
    )

    val T = true
    val F = false

    def assessment: GameAssessment =
      import GameAssessment.*
      val assessment = flags match
        //               SF1 SF2 BLR1 BLR2 HCMT MCMT NFM Holds
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
        case PlayerFlags(T, _, _, _, _, T, T, _) =>
          Unclear // high accuracy, consistent move times, no fast moves
        case PlayerFlags(T, _, _, F, _, F, T, _) =>
          Unclear // high accuracy, no fast moves, but doesn't blur or flat line

        case PlayerFlags(T, _, _, _, _, _, F, _) => UnlikelyCheating // high accuracy, but has fast moves

        case PlayerFlags(F, F, _, _, _, _, _, _) => NotCheating // low accuracy, doesn't hold advantage
        case _                                   => NotCheating

      if flags.suspiciousHoldAlert then assessment
      else if ~game.wonBy(color) then antichessCorrectedGameAssessment(assessment, pov, flags)
      else if assessment == Cheating then LikelyCheating
      else if assessment == LikelyCheating then Unclear
      else assessment

    val tcFactor: Double = game.speed match
      case Speed.Bullet | Speed.Blitz => 1.25
      case Speed.Rapid                => 1.0
      case Speed.Classical            => 0.6
      case _                          => 1.0
    PlayerAssessment(
      _id = s"${game.id}/${color.name}",
      gameId = game.id,
      userId = game.player(color).userId.err(s"PlayerAssessment $game $color no userId"),
      color = color,
      assessment = assessment,
      date = nowInstant,
      basics = basics,
      analysis = intAvgSd(AccuracyCP.diffsList(pov.sideAndStart, analysis).flatten),
      flags = flags,
      tcFactor = tcFactor.some
    )
