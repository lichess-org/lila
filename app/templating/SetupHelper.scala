package lila.app
package templating

import chess.{ Mode, Speed }
import lila.api.Context
import lila.i18n.{ I18nKeys => trans }
import lila.pref.Pref
import lila.report.Reason
import lila.setup.TimeMode
import lila.tournament.System

trait SetupHelper { self: I18nHelper =>

  type SelectChoice = (String, String, Option[String])

  val clockTimeChoices: List[SelectChoice] = List(
    ("0", "0", none),
    ("0.25", "¼", none),
    ("0.5", "½", none),
    ("0.75", "¾", none)
  ) ::: List(
      "1", "1.5", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
      "17", "18", "19", "20", "25", "30", "35", "40", "45", "60", "90", "120", "150", "180"
    ).map { v => (v.toString, v.toString, none) }

  val clockIncrementChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  } map { s =>
    (s.toString, s.toString, none)
  }

  val corresDaysChoices: List[SelectChoice] =
    ("1", "One day", none) :: List(2, 3, 5, 7, 10, 14).map { d =>
      (d.toString, s"${d} days", none)
    }

  def translatedTimeModeChoices(implicit ctx: Context) = List(
    (TimeMode.RealTime.id.toString, trans.realTime.txt(), none),
    (TimeMode.Correspondence.id.toString, trans.correspondence.txt(), none),
    (TimeMode.Unlimited.id.toString, trans.unlimited.txt(), none)
  )

  def translatedReasonChoices(implicit ctx: Context) = List(
    (Reason.Cheat.key, trans.cheat.txt()),
    (Reason.Insult.key, trans.insult.txt()),
    (Reason.Troll.key, trans.troll.txt()),
    (Reason.Other.key, trans.other.txt())
  )

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casual.txt(), none),
    (Mode.Rated.id.toString, trans.rated.txt(), none)
  )

  def translatedModeChoicesTournament(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casualTournament.txt(), none),
    (Mode.Rated.id.toString, trans.ratedTournament.txt(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena"
  )

  private def variantTuple(variant: chess.variant.Variant)(implicit ctx: Context): SelectChoice =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (chess.variant.Standard.id.toString, trans.standard.txt(), chess.variant.Standard.title.some)
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
      s.toString + " - " + trans.lessThanNbMinutes.pluralSameTxt(minutes),
      none
    )
  }

  def translatedSideChoices(implicit ctx: Context) = List(
    ("black", trans.black.txt(), none),
    ("random", trans.randomColor.txt(), none),
    ("white", trans.white.txt(), none)
  )

  def translatedAnimationChoices(implicit ctx: Context) = List(
    (Pref.Animation.NONE, trans.none.txt()),
    (Pref.Animation.FAST, trans.fast.txt()),
    (Pref.Animation.NORMAL, trans.normal.txt()),
    (Pref.Animation.SLOW, trans.slow.txt())
  )

  def translatedBoardCoordinateChoices(implicit ctx: Context) = List(
    (Pref.Coords.NONE, trans.no.txt()),
    (Pref.Coords.INSIDE, trans.insideTheBoard.txt()),
    (Pref.Coords.OUTSIDE, trans.outsideTheBoard.txt())
  )

  def translatedMoveListWhilePlayingChoices(implicit ctx: Context) = List(
    (Pref.Replay.NEVER, trans.never.txt()),
    (Pref.Replay.SLOW, trans.onSlowGames.txt()),
    (Pref.Replay.ALWAYS, trans.always.txt())
  )

  def translatedPieceNotationChoices(implicit ctx: Context) = List(
    (Pref.PieceNotation.SYMBOL, trans.chessPieceSymbol.txt()),
    (Pref.PieceNotation.LETTER, trans.pgnLetter.txt())
  )

  def translatedClockTenthsChoices(implicit ctx: Context) = List(
    (Pref.ClockTenths.NEVER, trans.never.txt()),
    (Pref.ClockTenths.LOWTIME, trans.whenTimeRemainingLessThanTenSeconds.txt()),
    (Pref.ClockTenths.ALWAYS, trans.always.txt())
  )

  def translatedMoveEventChoices(implicit ctx: Context) = List(
    (Pref.MoveEvent.CLICK, trans.clickTwoSquares.txt()),
    (Pref.MoveEvent.DRAG, trans.dragPiece.txt()),
    (Pref.MoveEvent.BOTH, trans.bothClicksAndDrag.txt())
  )

  def translatedTakebackChoices(implicit ctx: Context) = List(
    (Pref.Takeback.NEVER, trans.never.txt()),
    (Pref.Takeback.ALWAYS, trans.always.txt()),
    (Pref.Takeback.CASUAL, trans.inCasualGamesOnly.txt())
  )

  def translatedAutoQueenChoices(implicit ctx: Context) = List(
    (Pref.AutoQueen.NEVER, trans.never.txt()),
    (Pref.AutoQueen.PREMOVE, trans.whenPremoving.txt()),
    (Pref.AutoQueen.ALWAYS, trans.always.txt())
  )

  def translatedAutoThreefoldChoices(implicit ctx: Context) = List(
    (Pref.AutoThreefold.NEVER, trans.never.txt()),
    (Pref.AutoThreefold.ALWAYS, trans.always.txt()),
    (Pref.AutoThreefold.TIME, trans.whenTimeRemainingLessThanThirtySeconds.txt())
  )

  def submitMoveChoices(implicit ctx: Context) = List(
    (Pref.SubmitMove.NEVER, trans.never.txt()),
    (Pref.SubmitMove.CORRESPONDENCE_ONLY, trans.inCorrespondenceGames.txt()),
    (Pref.SubmitMove.CORRESPONDENCE_UNLIMITED, trans.correspondenceAndUnlimited.txt()),
    (Pref.SubmitMove.ALWAYS, trans.always.txt())
  )

  def confirmResignChoices(implicit ctx: Context) = List(
    (Pref.ConfirmResign.NO, trans.no.txt()),
    (Pref.ConfirmResign.YES, trans.yes.txt())
  )

  def translatedRookCastleChoices(implicit ctx: Context) = List(
    (Pref.RookCastle.NO, trans.castleByMovingTwoSquares.txt()),
    (Pref.RookCastle.YES, trans.castleByMovingOntoTheRook.txt())
  )

  def translatedChallengeChoices(implicit ctx: Context) = List(
    (Pref.Challenge.NEVER, trans.never.txt()),
    (Pref.Challenge.RATING, trans.ifRatingIsPlusMinusX.txt(lila.pref.Pref.Challenge.ratingThreshold)),
    (Pref.Challenge.FRIEND, trans.onlyFriends.txt()),
    (Pref.Challenge.ALWAYS, trans.always.txt())
  )

  def translatedMessageChoices(implicit ctx: Context) = List(
    (Pref.Message.NEVER, trans.never.txt()),
    (Pref.Message.FRIEND, trans.onlyFriends.txt()),
    (Pref.Message.ALWAYS, trans.always.txt())
  )

  def translatedStudyInviteChoices(implicit ctx: Context) = List(
    (Pref.StudyInvite.NEVER, trans.never.txt()),
    (Pref.StudyInvite.FRIEND, trans.onlyFriends.txt()),
    (Pref.StudyInvite.ALWAYS, trans.always.txt())
  )

  def translatedInsightSquareChoices(implicit ctx: Context) = List(
    (Pref.InsightShare.NOBODY, trans.withNobody.txt()),
    (Pref.InsightShare.FRIENDS, trans.withFriends.txt()),
    (Pref.InsightShare.EVERYBODY, trans.withEverybody.txt())
  )

  def translatedBoardResizeHandleChoices(implicit ctx: Context) = List(
    (Pref.ResizeHandle.NEVER, trans.never.txt()),
    (Pref.ResizeHandle.INITIAL, trans.onlyOnInitialPosition.txt()),
    (Pref.ResizeHandle.ALWAYS, trans.always.txt())
  )

  def translatedBlindfoldChoices(implicit ctx: Context) = List(
    Pref.Blindfold.NO -> trans.no.txt(),
    Pref.Blindfold.YES -> trans.yes.txt()
  )
}
