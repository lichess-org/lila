package lila.app
package templating

import chess.{ Mode, Speed }
import lila.api.Context
import lila.i18n.I18nKeys
import lila.pref.Pref
import lila.report.Reason
import lila.setup.TimeMode
import lila.tournament.System

trait SetupHelper { self: I18nHelper =>

  def translatedTimeModeChoices(implicit ctx: Context) = List(
    (TimeMode.RealTime.id.toString, I18nKeys.realTime.str(), none),
    (TimeMode.Correspondence.id.toString, I18nKeys.correspondence.str(), none),
    (TimeMode.Unlimited.id.toString, I18nKeys.unlimited.str(), none)
  )

  def translatedReasonChoices(implicit ctx: Context) = List(
    (Reason.Cheat.key, I18nKeys.cheat.str()),
    (Reason.Insult.key, I18nKeys.insult.str()),
    (Reason.Troll.key, I18nKeys.troll.str()),
    (Reason.Other.key, I18nKeys.other.str())
  )

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, I18nKeys.casual.str(), none),
    (Mode.Rated.id.toString, I18nKeys.rated.str(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena"
  )

  private def variantTuple(variant: chess.variant.Variant)(implicit ctx: Context): (String, String, Option[String]) =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (chess.variant.Standard.id.toString, I18nKeys.standard.str(), chess.variant.Standard.title.some)
  )

  def translatedVariantChoicesWithVariants(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.Crazyhouse) :+
      variantTuple(chess.variant.Chess960) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.ThreeCheck) :+
      variantTuple(chess.variant.Antichess) :+
      variantTuple(chess.variant.Atomic) :+
      variantTuple(chess.variant.Horde) :+
      variantTuple(chess.variant.RacingKings)

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.Chess960) :+
      variantTuple(chess.variant.FromPosition)

  def translatedAiVariantChoices(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.Crazyhouse) :+
      variantTuple(chess.variant.Chess960) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.ThreeCheck) :+
      variantTuple(chess.variant.Antichess) :+
      variantTuple(chess.variant.Atomic) :+
      variantTuple(chess.variant.Horde) :+
      variantTuple(chess.variant.RacingKings) :+
      variantTuple(chess.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(chess.variant.FromPosition)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.limited map { s =>
    val minutes = s.range.max / 60 + 1
    (
      s.id.toString,
      s.toString + " - " + I18nKeys.lessThanNbMinutes.pluralStr(minutes, minutes),
      none
    )
  }

  def translatedAnimationChoices(implicit ctx: Context) = List(
    (Pref.Animation.NONE, I18nKeys.none.str()),
    (Pref.Animation.FAST, I18nKeys.fast.str()),
    (Pref.Animation.NORMAL, I18nKeys.normal.str()),
    (Pref.Animation.SLOW, I18nKeys.slow.str())
  )

  def translatedBoardCoordinateChoices(implicit ctx: Context) = List(
    (Pref.Coords.NONE, I18nKeys.no.str()),
    (Pref.Coords.INSIDE, I18nKeys.insideTheBoard.str()),
    (Pref.Coords.OUTSIDE, I18nKeys.outsideTheBoard.str())
  )

  def translatedMoveListWhilePlayingChoices(implicit ctx: Context) = List(
    (Pref.Replay.NEVER, I18nKeys.never.str()),
    (Pref.Replay.SLOW, I18nKeys.onSlowGames.str()),
    (Pref.Replay.ALWAYS, I18nKeys.always.str())
  )

  def translatedClockTenthsChoices(implicit ctx: Context) = List(
    (Pref.ClockTenths.NEVER, I18nKeys.never.str()),
    (Pref.ClockTenths.LOWTIME, I18nKeys.whenTimeRemainingLessThanTenSeconds.str()),
    (Pref.ClockTenths.ALWAYS, I18nKeys.always.str())
  )

  def translatedTakebackChoices(implicit ctx: Context) = List(
    (Pref.Takeback.NEVER, I18nKeys.never.str()),
    (Pref.Takeback.ALWAYS, I18nKeys.always.str()),
    (Pref.Takeback.CASUAL, I18nKeys.inCasualGamesOnly.str())
  )

  def translatedAutoQueenChoices(implicit ctx: Context) = List(
    (Pref.AutoQueen.NEVER, I18nKeys.never.str()),
    (Pref.AutoQueen.PREMOVE, I18nKeys.whenPremoving.str()),
    (Pref.AutoQueen.ALWAYS, I18nKeys.always.str())
  )

  def translatedAutoThreefoldChoices(implicit ctx: Context) = List(
    (Pref.AutoThreefold.NEVER, I18nKeys.never.str()),
    (Pref.AutoThreefold.ALWAYS, I18nKeys.always.str()),
    (Pref.AutoThreefold.TIME, I18nKeys.whenTimeRemainingLessThanThirtySeconds.str())
  )

  def submitMoveChoices(implicit ctx: Context) = List(
    (Pref.SubmitMove.NEVER, I18nKeys.never.str()),
    (Pref.SubmitMove.CORRESPONDENCE_ONLY, I18nKeys.inCorrespondenceGames.str()),
    (Pref.SubmitMove.CORRESPONDENCE_UNLIMITED, I18nKeys.correspondenceAndUnlimited.str()),
    (Pref.SubmitMove.ALWAYS, I18nKeys.always.str())
  )

  def confirmResignChoices(implicit ctx: Context) = List(
    (Pref.ConfirmResign.NO, I18nKeys.no.str()),
    (Pref.ConfirmResign.YES, I18nKeys.yes.str())
  )

  def translatedChallengeChoices(implicit ctx: Context) = List(
    (Pref.Challenge.NEVER, I18nKeys.never.str()),
    (Pref.Challenge.RATING, I18nKeys.ifRatingIsPlusMinusX.literalStr(lila.pref.Pref.Challenge.ratingThreshold)),
    (Pref.Challenge.FRIEND, I18nKeys.onlyFriends.str()),
    (Pref.Challenge.ALWAYS, I18nKeys.always.str())
  )

  def translatedMessageChoices(implicit ctx: Context) = List(
    (Pref.Message.NEVER, I18nKeys.never.str()),
    (Pref.Message.FRIEND, I18nKeys.onlyFriends.str()),
    (Pref.Message.ALWAYS, I18nKeys.always.str())
  )

  def translatedStudyInviteChoices(implicit ctx: Context) = List(
    (Pref.StudyInvite.NEVER, I18nKeys.never.str()),
    (Pref.StudyInvite.FRIEND, I18nKeys.onlyFriends.str()),
    (Pref.StudyInvite.ALWAYS, I18nKeys.always.str())
  )

  def translatedBlindfoldChoices(implicit ctx: Context) = List(
    Pref.Blindfold.NO -> I18nKeys.no.str(),
    Pref.Blindfold.YES -> I18nKeys.yes.str()
  )
}
