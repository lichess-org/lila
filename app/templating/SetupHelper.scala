package lila.app
package templating

import chess.{ Mode, Speed }
import lila.api.Context
import lila.pref.Pref
import lila.pref.Pref.Difficulty
import lila.report.Reason
import lila.setup.TimeMode
import lila.tournament.System

trait SetupHelper { self: I18nHelper =>

  def translatedTimeModeChoices(implicit ctx: Context) = List(
    (TimeMode.RealTime.id.toString, trans.realTime.str(), none),
    (TimeMode.Correspondence.id.toString, trans.correspondence.str(), none),
    (TimeMode.Unlimited.id.toString, trans.unlimited.str(), none)
  )

  def translatedReasonChoices(implicit ctx: Context) = List(
    (Reason.Cheat.name, trans.cheat.str()),
    (Reason.Insult.name, trans.insult.str()),
    (Reason.Troll.name, trans.troll.str()),
    (Reason.Other.name, trans.other.str())
  )

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casual.str(), none),
    (Mode.Rated.id.toString, trans.rated.str(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena"
  )

  private def variantTuple(variant: chess.variant.Variant)(implicit ctx: Context): (String, String, Option[String]) =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (chess.variant.Standard.id.toString, trans.standard.str(), chess.variant.Standard.title.some),
    variantTuple(chess.variant.Chess960)
  )

  def translatedVariantChoicesWithVariants(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.ThreeCheck) :+
      variantTuple(chess.variant.Antichess) :+
      variantTuple(chess.variant.Atomic) :+
      variantTuple(chess.variant.Horde)

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.FromPosition)

  def translatedAiVariantChoices(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.ThreeCheck) :+
      variantTuple(chess.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(chess.variant.FromPosition)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.limited map { s =>
    (s.id.toString, {
      (s.range.min, s.range.max) match {
        case (0, y) => s.toString + " - " + trans.lessThanNbMinutes(y / 60 + 1)
        case (x, y) => s.toString + " - " + trans.xToYMinutes(x / 60, y / 60 + 1)
      }
    }, none)
  }

  def translatedAnimationChoices(implicit ctx: Context) = List(
    (Pref.Animation.NONE, trans.none.str()),
    (Pref.Animation.FAST, trans.fast.str()),
    (Pref.Animation.NORMAL, trans.normal.str()),
    (Pref.Animation.SLOW, trans.slow.str())
  )

  def translatedBoardCoordinateChoices(implicit ctx: Context) = List(
    (Pref.Coords.NONE, trans.no.str()),
    (Pref.Coords.INSIDE, trans.insideTheBoard.str()),
    (Pref.Coords.OUTSIDE, trans.outsideTheBoard.str())
  )

  def translatedMoveListWhilePlayingChoices(implicit ctx: Context) = List(
    (Pref.Replay.NEVER, trans.never.str()),
    (Pref.Replay.SLOW, trans.onSlowGames.str()),
    (Pref.Replay.ALWAYS, trans.always.str())
  )

  def translatedClockTenthsChoices(implicit ctx: Context) = List(
    (Pref.ClockTenths.NEVER, trans.never.str()),
    (Pref.ClockTenths.LOWTIME, trans.whenTimeRemainingLessThanTenSeconds.str()),
    (Pref.ClockTenths.ALWAYS, trans.always.str())
  )

  def translatedTakebackChoices(implicit ctx: Context) = List(
    (Pref.Takeback.NEVER, trans.never.str()),
    (Pref.Takeback.ALWAYS, trans.always.str()),
    (Pref.Takeback.CASUAL, trans.inCasualGamesOnly.str())
  )

  def translatedMoreTimeChoices(implicit ctx: Context) = List(
    (Pref.MoreTimeAble.NEVER, trans.never.str()),
    (Pref.MoreTimeAble.ALWAYS, trans.always.str()),
    (Pref.MoreTimeAble.CASUAL, trans.inCasualGamesOnly.str())
  )

  def translatedAutoQueenChoices(implicit ctx: Context) = List(
    (Pref.AutoQueen.NEVER, trans.never.str()),
    (Pref.AutoQueen.PREMOVE, trans.whenPremoving.str()),
    (Pref.AutoQueen.ALWAYS, trans.always.str())
  )

  def translatedAutoThreefoldChoices(implicit ctx: Context) = List(
    (Pref.AutoThreefold.NEVER, trans.never.str()),
    (Pref.AutoThreefold.ALWAYS, trans.always.str()),
    (Pref.AutoThreefold.TIME, trans.whenTimeRemainingLessThanThirtySeconds.str())
  )

  def translatedDifficultyChoices(implicit ctx: Context) = List(
    (Pref.Difficulty.EASY, trans.difficultyEasy.str()),
    (Pref.Difficulty.NORMAL, trans.difficultyNormal.str()),
    (Pref.Difficulty.HARD, trans.difficultyHard.str())
  )

  def submitMoveChoices(implicit ctx: Context) = List(
    (Pref.SubmitMove.NEVER, trans.never.str()),
    (Pref.SubmitMove.CORRESPONDENCE, trans.inCorrespondenceGames.str()),
    (Pref.SubmitMove.ALWAYS, trans.always.str())
  )

  def translatedChallengeChoices(implicit ctx: Context) = List(
    (Pref.Challenge.NEVER, trans.never.str()),
    (Pref.Challenge.RATING, trans.ifRatingIsPlusMinusX(500).toString()),
    (Pref.Challenge.FRIEND, trans.onlyFriends.str()),
    (Pref.Challenge.ALWAYS, trans.always.str())
  )

}
