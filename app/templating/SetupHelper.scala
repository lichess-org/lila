package lila.app
package templating

import chess.variant.Variant
import chess.{ Mode, Speed }
import play.api.i18n.Lang

import lila.core.i18n.I18nKey as trans
import lila.pref.Pref
import lila.report.Reason
import lila.setup.TimeMode
import lila.core.i18n.Translate

trait SetupHelper:
  self: I18nHelper =>

  type SelectChoice = (String, String, Option[String])

  val clockTimeChoices: List[SelectChoice] = List(
    ("0", "0", none),
    ("0.25", "¼", none),
    ("0.5", "½", none),
    ("0.75", "¾", none)
  ) ::: List(
    "1",
    "1.5",
    "2",
    "3",
    "4",
    "5",
    "6",
    "7",
    "8",
    "9",
    "10",
    "11",
    "12",
    "13",
    "14",
    "15",
    "16",
    "17",
    "18",
    "19",
    "20",
    "25",
    "30",
    "35",
    "40",
    "45",
    "60",
    "75",
    "90",
    "105",
    "120",
    "135",
    "150",
    "165",
    "180"
  ).map: v =>
    (v, v, none)

  val clockIncrementChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  }.map { s =>
    (s.toString, s.toString, none)
  }

  val corresDaysChoices: List[SelectChoice] =
    ("1", "One day", none) :: List(2, 3, 5, 7, 10, 14).map { d =>
      (d.toString, s"$d days", none)
    }

  def translatedTimeModeChoices(using Translate) =
    List(
      (TimeMode.RealTime.id.toString, trans.site.realTime.txt(), none),
      (TimeMode.Correspondence.id.toString, trans.site.correspondence.txt(), none),
      (TimeMode.Unlimited.id.toString, trans.site.unlimited.txt(), none)
    )

  def translatedReasonChoices(using Translate) =
    List(
      (Reason.Cheat.key, trans.site.cheat.txt()),
      (Reason.Comm.key, trans.site.insult.txt()),
      (Reason.Boost.key, trans.site.ratingManipulation.txt()),
      (Reason.Comm.key, trans.site.troll.txt()),
      (Reason.Sexism.key, "Sexual harassment or Sexist remarks"),
      (Reason.Username.key, trans.site.username.txt()),
      (Reason.Other.key, trans.site.other.txt())
    )

  def translatedModeChoices(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casual.txt(), none),
      (Mode.Rated.id.toString, trans.site.rated.txt(), none)
    )

  def translatedIncrementChoices(using Translate) =
    List(
      (1, trans.site.yes.txt(), none),
      (0, trans.site.no.txt(), none)
    )

  def translatedModeChoicesTournament(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casualTournament.txt(), none),
      (Mode.Rated.id.toString, trans.site.ratedTournament.txt(), none)
    )

  private val encodeId = (v: Variant) => v.id.toString

  private def variantTupleId = variantTuple(encodeId)

  private def variantTuple(encode: Variant => String)(variant: Variant) =
    (encode(variant), variant.name, variant.title.some)

  def translatedVariantChoices(using Translate): List[SelectChoice] =
    translatedVariantChoices(encodeId)

  def translatedVariantChoices(encode: Variant => String)(using Translate): List[SelectChoice] =
    List(
      (encode(chess.variant.Standard), trans.site.standard.txt(), chess.variant.Standard.title.some)
    )

  def translatedVariantChoicesWithVariants(using Translate): List[SelectChoice] =
    translatedVariantChoicesWithVariants(encodeId)

  def translatedVariantChoicesWithVariants(
      encode: Variant => String
  )(using Translate): List[SelectChoice] =
    translatedVariantChoices(encode) ::: List(
      chess.variant.Crazyhouse,
      chess.variant.Chess960,
      chess.variant.KingOfTheHill,
      chess.variant.ThreeCheck,
      chess.variant.Antichess,
      chess.variant.Atomic,
      chess.variant.Horde,
      chess.variant.RacingKings
    ).map(variantTuple(encode))

  def translatedVariantChoicesWithFen(using Translate) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.FromPosition)

  def translatedAiVariantChoices(using Translate) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Crazyhouse) :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.KingOfTheHill) :+
      variantTupleId(chess.variant.ThreeCheck) :+
      variantTupleId(chess.variant.Antichess) :+
      variantTupleId(chess.variant.Atomic) :+
      variantTupleId(chess.variant.Horde) :+
      variantTupleId(chess.variant.RacingKings) :+
      variantTupleId(chess.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(using Translate) =
    translatedVariantChoicesWithVariants :+
      variantTupleId(chess.variant.FromPosition)

  def translatedSpeedChoices(using Translate) =
    Speed.limited.map { s =>
      val minutes = s.range.max / 60 + 1
      (
        s.id.toString,
        s.toString + " - " + trans.site.lessThanNbMinutes.pluralSameTxt(minutes),
        none
      )
    }

  def translatedSideChoices(using Translate) =
    List(
      ("black", trans.site.black.txt(), none),
      ("random", trans.site.randomColor.txt(), none),
      ("white", trans.site.white.txt(), none)
    )

  def translatedAnimationChoices(using Translate) =
    List(
      (Pref.Animation.NONE, trans.site.none.txt()),
      (Pref.Animation.FAST, trans.site.fast.txt()),
      (Pref.Animation.NORMAL, trans.site.normal.txt()),
      (Pref.Animation.SLOW, trans.site.slow.txt())
    )

  def translatedZenChoices(using Translate) =
    List(
      (Pref.Zen.NO, trans.site.no.txt()),
      (Pref.Zen.YES, trans.site.yes.txt()),
      (Pref.Zen.GAME_AUTO, trans.preferences.inGameOnly.txt())
    )

  def translatedBoardCoordinateChoices(using Translate) =
    List(
      (Pref.Coords.NONE, trans.site.no.txt()),
      (Pref.Coords.INSIDE, trans.site.insideTheBoard.txt()),
      (Pref.Coords.OUTSIDE, trans.site.outsideTheBoard.txt())
    )

  def translatedMoveListWhilePlayingChoices(using Translate) =
    List(
      (Pref.Replay.NEVER, trans.site.never.txt()),
      (Pref.Replay.SLOW, trans.site.onSlowGames.txt()),
      (Pref.Replay.ALWAYS, trans.site.always.txt())
    )

  def translatedPieceNotationChoices(using Translate) =
    List(
      (Pref.PieceNotation.SYMBOL, trans.preferences.chessPieceSymbol.txt()),
      (Pref.PieceNotation.LETTER, trans.preferences.pgnLetter.txt())
    )

  def translatedClockTenthsChoices(using Translate) =
    List(
      (Pref.ClockTenths.NEVER, trans.site.never.txt()),
      (Pref.ClockTenths.LOWTIME, trans.preferences.whenTimeRemainingLessThanTenSeconds.txt()),
      (Pref.ClockTenths.ALWAYS, trans.site.always.txt())
    )

  def translatedMoveEventChoices(using Translate) =
    List(
      (Pref.MoveEvent.CLICK, trans.preferences.clickTwoSquares.txt()),
      (Pref.MoveEvent.DRAG, trans.preferences.dragPiece.txt()),
      (Pref.MoveEvent.BOTH, trans.preferences.bothClicksAndDrag.txt())
    )

  def translatedTakebackChoices(using Translate) =
    List(
      (Pref.Takeback.NEVER, trans.site.never.txt()),
      (Pref.Takeback.ALWAYS, trans.site.always.txt()),
      (Pref.Takeback.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def translatedMoretimeChoices(using Translate) =
    List(
      (Pref.Moretime.NEVER, trans.site.never.txt()),
      (Pref.Moretime.ALWAYS, trans.site.always.txt()),
      (Pref.Moretime.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def translatedAutoQueenChoices(using Translate) =
    List(
      (Pref.AutoQueen.NEVER, trans.site.never.txt()),
      (Pref.AutoQueen.PREMOVE, trans.preferences.whenPremoving.txt()),
      (Pref.AutoQueen.ALWAYS, trans.site.always.txt())
    )

  def translatedAutoThreefoldChoices(using Translate) =
    List(
      (Pref.AutoThreefold.NEVER, trans.site.never.txt()),
      (Pref.AutoThreefold.ALWAYS, trans.site.always.txt()),
      (Pref.AutoThreefold.TIME, trans.preferences.whenTimeRemainingLessThanThirtySeconds.txt())
    )

  def submitMoveChoices(using Translate) =
    List(
      (Pref.SubmitMove.UNLIMITED, trans.site.unlimited.txt()),
      (Pref.SubmitMove.CORRESPONDENCE, trans.site.correspondence.txt()),
      (Pref.SubmitMove.CLASSICAL, trans.site.classical.txt()),
      (Pref.SubmitMove.RAPID, trans.site.rapid.txt()),
      (Pref.SubmitMove.BLITZ, trans.site.blitz.txt())
    )

  def confirmResignChoices(using Translate) =
    List(
      (Pref.ConfirmResign.NO, trans.site.no.txt()),
      (Pref.ConfirmResign.YES, trans.site.yes.txt())
    )

  def translatedRookCastleChoices(using Translate) =
    List(
      (Pref.RookCastle.NO, trans.preferences.castleByMovingTwoSquares.txt()),
      (Pref.RookCastle.YES, trans.preferences.castleByMovingOntoTheRook.txt())
    )

  def translatedChallengeChoices(using Translate) =
    List(
      (lila.core.pref.Challenge.NEVER, trans.site.never.txt()),
      (
        lila.core.pref.Challenge.RATING,
        trans.site.ifRatingIsPlusMinusX.txt(lila.pref.Pref.Challenge.ratingThreshold)
      ),
      (lila.core.pref.Challenge.FRIEND, trans.site.onlyFriends.txt()),
      (lila.core.pref.Challenge.REGISTERED, trans.site.ifRegistered.txt()),
      (lila.core.pref.Challenge.ALWAYS, trans.site.always.txt())
    )

  def translatedMessageChoices(using Translate) =
    List(
      (lila.core.pref.Message.NEVER, trans.site.onlyExistingConversations.txt()),
      (lila.core.pref.Message.FRIEND, trans.site.onlyFriends.txt()),
      (lila.core.pref.Message.ALWAYS, trans.site.always.txt())
    )

  def translatedStudyInviteChoices(using Translate) = privacyBaseChoices
  def translatedPalantirChoices(using Translate)    = privacyBaseChoices
  private def privacyBaseChoices(using Translate) =
    List(
      (lila.core.pref.StudyInvite.NEVER, trans.site.never.txt()),
      (lila.core.pref.StudyInvite.FRIEND, trans.site.onlyFriends.txt()),
      (lila.core.pref.StudyInvite.ALWAYS, trans.site.always.txt())
    )

  def translatedInsightShareChoices(using Translate) =
    List(
      (lila.core.pref.InsightShare.NOBODY, trans.site.withNobody.txt()),
      (lila.core.pref.InsightShare.FRIENDS, trans.site.withFriends.txt()),
      (lila.core.pref.InsightShare.EVERYBODY, trans.site.withEverybody.txt())
    )

  def translatedBoardResizeHandleChoices(using Translate) =
    List(
      (Pref.ResizeHandle.NEVER, trans.site.never.txt()),
      (Pref.ResizeHandle.INITIAL, trans.preferences.onlyOnInitialPosition.txt()),
      (Pref.ResizeHandle.ALWAYS, trans.site.always.txt())
    )

  def translatedBooleanIntChoices(using Translate) =
    List(
      0 -> trans.site.no.txt(),
      1 -> trans.site.yes.txt()
    )

  def translatedBooleanChoices(using Translate) =
    List(
      false -> trans.site.no.txt(),
      true  -> trans.site.yes.txt()
    )
