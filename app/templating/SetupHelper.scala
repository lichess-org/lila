package lidraughts.app
package templating

import draughts.{ Mode, Speed }
import lidraughts.api.Context
import lidraughts.i18n.I18nKeys
import lidraughts.pref.Pref
import lidraughts.report.Reason
import lidraughts.setup.TimeMode
import lidraughts.tournament.System

trait SetupHelper { self: I18nHelper =>

  def translatedTimeModeChoices(implicit ctx: Context) = List(
    (TimeMode.RealTime.id.toString, I18nKeys.realTime.txt(), none),
    (TimeMode.Correspondence.id.toString, I18nKeys.correspondence.txt(), none),
    (TimeMode.Unlimited.id.toString, I18nKeys.unlimited.txt(), none)
  )

  def translatedReasonChoices(implicit ctx: Context) = List(
    (Reason.Cheat.key, I18nKeys.cheat.txt()),
    (Reason.Insult.key, I18nKeys.insult.txt()),
    (Reason.Troll.key, I18nKeys.troll.txt()),
    (Reason.Other.key, I18nKeys.other.txt())
  )

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, I18nKeys.casual.txt(), none),
    (Mode.Rated.id.toString, I18nKeys.rated.txt(), none)
  )

  def translatedModeChoicesTournament(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, I18nKeys.casualTournament.txt(), none),
    (Mode.Rated.id.toString, I18nKeys.ratedTournament.txt(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena"
  )

  def translatedColorChoices(implicit ctx: Context) = List(
    "white" -> I18nKeys.white.txt(),
    "random" -> I18nKeys.randomColor.txt(),
    "black" -> I18nKeys.black.txt()
  )

  def translatedChatChoices(implicit ctx: Context) = List(
    "everyone" -> I18nKeys.everyone.txt(),
    "spectators" -> I18nKeys.spectatorsOnly.txt(),
    "participants" -> I18nKeys.participantsOnly.txt()
  )

  private def variantTuple(variant: draughts.variant.Variant)(implicit ctx: Context): (String, String, Option[String]) =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (draughts.variant.Standard.id.toString, I18nKeys.standard.txt(), draughts.variant.Standard.title.some)
  )

  def translatedVariantChoicesWithVariants(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(draughts.variant.Frisian) :+
      variantTuple(draughts.variant.Frysk) :+
      variantTuple(draughts.variant.Antidraughts) :+
      variantTuple(draughts.variant.Breakthrough)

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(draughts.variant.FromPosition)

  def translatedAiVariantChoices(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(draughts.variant.Breakthrough) :+
      variantTuple(draughts.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(draughts.variant.FromPosition)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.limited map { s =>
    val minutes = s.range.max / 60 + 1
    (
      s.id.toString,
      s.toString + " - " + I18nKeys.lessThanNbMinutes.pluralSameTxt(minutes),
      none
    )
  }

  def translatedAnimationChoices(implicit ctx: Context) = List(
    (Pref.Animation.NONE, I18nKeys.none.txt()),
    (Pref.Animation.FAST, I18nKeys.fast.txt()),
    (Pref.Animation.NORMAL, I18nKeys.normal.txt()),
    (Pref.Animation.SLOW, I18nKeys.slow.txt())
  )

  def translatedBoardCoordinateChoices(implicit ctx: Context) = List(
    (Pref.Coords.NONE, I18nKeys.no.txt()),
    (Pref.Coords.INSIDE, I18nKeys.insideTheBoard.txt()),
    (Pref.Coords.OUTSIDE, I18nKeys.outsideTheBoard.txt())
  )

  def translatedMoveListWhilePlayingChoices(implicit ctx: Context) = List(
    (Pref.Replay.NEVER, I18nKeys.never.txt()),
    (Pref.Replay.SLOW, I18nKeys.onSlowGames.txt()),
    (Pref.Replay.ALWAYS, I18nKeys.always.txt())
  )

  def translatedGameResultNotationChoices(implicit ctx: Context) =
    Pref.GameResult.choices.toList

  def translatedClockTenthsChoices(implicit ctx: Context) = List(
    (Pref.ClockTenths.NEVER, I18nKeys.never.txt()),
    (Pref.ClockTenths.LOWTIME, I18nKeys.whenTimeRemainingLessThanTenSeconds.txt()),
    (Pref.ClockTenths.ALWAYS, I18nKeys.always.txt())
  )

  def translatedMoveEventChoices(implicit ctx: Context) = List(
    (Pref.MoveEvent.CLICK, I18nKeys.clickTwoSquares.txt()),
    (Pref.MoveEvent.DRAG, I18nKeys.dragPiece.txt()),
    (Pref.MoveEvent.BOTH, I18nKeys.bothClicksAndDrag.txt())
  )

  def translatedTakebackChoices(implicit ctx: Context) = List(
    (Pref.Takeback.NEVER, I18nKeys.never.txt()),
    (Pref.Takeback.ALWAYS, I18nKeys.always.txt()),
    (Pref.Takeback.CASUAL, I18nKeys.inCasualGamesOnly.txt())
  )

  def translatedAutoThreefoldChoices(implicit ctx: Context) = List(
    (Pref.AutoThreefold.NEVER, I18nKeys.never.txt()),
    (Pref.AutoThreefold.ALWAYS, I18nKeys.always.txt()),
    (Pref.AutoThreefold.TIME, I18nKeys.whenTimeRemainingLessThanThirtySeconds.txt())
  )

  def submitMoveChoices(implicit ctx: Context) = List(
    (Pref.SubmitMove.NEVER, I18nKeys.never.txt()),
    (Pref.SubmitMove.CORRESPONDENCE_ONLY, I18nKeys.inCorrespondenceGames.txt()),
    (Pref.SubmitMove.CORRESPONDENCE_UNLIMITED, I18nKeys.correspondenceAndUnlimited.txt()),
    (Pref.SubmitMove.ALWAYS, I18nKeys.always.txt())
  )

  def confirmResignChoices(implicit ctx: Context) = List(
    (Pref.ConfirmResign.NO, I18nKeys.no.txt()),
    (Pref.ConfirmResign.YES, I18nKeys.yes.txt())
  )

  def translatedChallengeChoices(implicit ctx: Context) = List(
    (Pref.Challenge.NEVER, I18nKeys.never.txt()),
    (Pref.Challenge.RATING, I18nKeys.ifRatingIsPlusMinusX.txt(lidraughts.pref.Pref.Challenge.ratingThreshold)),
    (Pref.Challenge.FRIEND, I18nKeys.onlyFriends.txt()),
    (Pref.Challenge.ALWAYS, I18nKeys.always.txt())
  )

  def translatedMessageChoices(implicit ctx: Context) = List(
    (Pref.Message.NEVER, I18nKeys.never.txt()),
    (Pref.Message.FRIEND, I18nKeys.onlyFriends.txt()),
    (Pref.Message.ALWAYS, I18nKeys.always.txt())
  )

  def translatedStudyInviteChoices(implicit ctx: Context) = List(
    (Pref.StudyInvite.NEVER, I18nKeys.never.txt()),
    (Pref.StudyInvite.FRIEND, I18nKeys.onlyFriends.txt()),
    (Pref.StudyInvite.ALWAYS, I18nKeys.always.txt())
  )

  def translatedInsightSquareChoices(implicit ctx: Context) = List(
    (Pref.InsightShare.NOBODY, I18nKeys.withNobody.txt()),
    (Pref.InsightShare.FRIENDS, I18nKeys.withFriends.txt()),
    (Pref.InsightShare.EVERYBODY, I18nKeys.withEverybody.txt())
  )

  def translatedBlindfoldChoices(implicit ctx: Context) = List(
    Pref.Blindfold.NO -> I18nKeys.no.txt(),
    Pref.Blindfold.YES -> I18nKeys.yes.txt()
  )
}
