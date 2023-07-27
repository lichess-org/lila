package lila.app
package templating

import chess.{ Mode, Speed }
import chess.variant.Variant
import play.api.i18n.Lang

import lila.i18n.{ I18nKeys as trans }
import lila.pref.Pref
import lila.report.Reason
import lila.setup.TimeMode

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
  } map { s =>
    (s.toString, s.toString, none)
  }

  val corresDaysChoices: List[SelectChoice] =
    ("1", "One day", none) :: List(2, 3, 5, 7, 10, 14).map { d =>
      (d.toString, s"$d days", none)
    }

  def translatedTimeModeChoices(using Lang) =
    List(
      (TimeMode.RealTime.id.toString, trans.realTime.txt(), none),
      (TimeMode.Correspondence.id.toString, trans.correspondence.txt(), none),
      (TimeMode.Unlimited.id.toString, trans.unlimited.txt(), none)
    )

  def translatedReasonChoices(using Lang) =
    List(
      (Reason.Cheat.key, trans.cheat.txt()),
      (Reason.Comm.key, trans.insult.txt()),
      (Reason.Boost.key, trans.ratingManipulation.txt()),
      (Reason.Comm.key, trans.troll.txt()),
      (Reason.Sexism.key, "Sexual harassment or Sexist remarks"),
      (Reason.Username.key, trans.username.txt()),
      (Reason.Other.key, trans.other.txt())
    )

  def translatedModeChoices(using Lang) =
    List(
      (Mode.Casual.id.toString, trans.casual.txt(), none),
      (Mode.Rated.id.toString, trans.rated.txt(), none)
    )

  def translatedIncrementChoices(using Lang) =
    List(
      (1, trans.yes.txt(), none),
      (0, trans.no.txt(), none)
    )

  def translatedModeChoicesTournament(using Lang) =
    List(
      (Mode.Casual.id.toString, trans.casualTournament.txt(), none),
      (Mode.Rated.id.toString, trans.ratedTournament.txt(), none)
    )

  private val encodeId = (v: Variant) => v.id.toString

  private def variantTupleId = variantTuple(encodeId)

  private def variantTuple(encode: Variant => String)(variant: Variant) =
    (encode(variant), variant.name, variant.title.some)

  def translatedVariantChoices(using Lang): List[SelectChoice] =
    translatedVariantChoices(encodeId)

  def translatedVariantChoices(encode: Variant => String)(using Lang): List[SelectChoice] =
    List(
      (encode(chess.variant.Standard), trans.standard.txt(), chess.variant.Standard.title.some)
    )

  def translatedVariantChoicesWithVariants(using Lang): List[SelectChoice] =
    translatedVariantChoicesWithVariants(encodeId)

  def translatedVariantChoicesWithVariants(
      encode: Variant => String
  )(using Lang): List[SelectChoice] =
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

  def translatedVariantChoicesWithFen(using Lang) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.FromPosition)

  def translatedAiVariantChoices(using Lang) =
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

  def translatedVariantChoicesWithVariantsAndFen(using Lang) =
    translatedVariantChoicesWithVariants :+
      variantTupleId(chess.variant.FromPosition)

  def translatedSpeedChoices(using Lang) =
    Speed.limited map { s =>
      val minutes = s.range.max / 60 + 1
      (
        s.id.toString,
        s.toString + " - " + trans.lessThanNbMinutes.pluralSameTxt(minutes),
        none
      )
    }

  def translatedSideChoices(using Lang) =
    List(
      ("black", trans.black.txt(), none),
      ("random", trans.randomColor.txt(), none),
      ("white", trans.white.txt(), none)
    )

  def translatedAnimationChoices(using Lang) =
    List(
      (Pref.Animation.NONE, trans.none.txt()),
      (Pref.Animation.FAST, trans.fast.txt()),
      (Pref.Animation.NORMAL, trans.normal.txt()),
      (Pref.Animation.SLOW, trans.slow.txt())
    )

  def translatedZenChoices(using Lang) =
    List(
      (Pref.Zen.NO, trans.no.txt()),
      (Pref.Zen.YES, trans.yes.txt()),
      (Pref.Zen.GAME_AUTO, trans.preferences.inGameOnly.txt())
    )

  def translatedBoardCoordinateChoices(using Lang) =
    List(
      (Pref.Coords.NONE, trans.no.txt()),
      (Pref.Coords.INSIDE, trans.insideTheBoard.txt()),
      (Pref.Coords.OUTSIDE, trans.outsideTheBoard.txt())
    )

  def translatedMoveListWhilePlayingChoices(using Lang) =
    List(
      (Pref.Replay.NEVER, trans.never.txt()),
      (Pref.Replay.SLOW, trans.onSlowGames.txt()),
      (Pref.Replay.ALWAYS, trans.always.txt())
    )

  def translatedPieceNotationChoices(using Lang) =
    List(
      (Pref.PieceNotation.SYMBOL, trans.preferences.chessPieceSymbol.txt()),
      (Pref.PieceNotation.LETTER, trans.preferences.pgnLetter.txt())
    )

  def translatedClockTenthsChoices(using Lang) =
    List(
      (Pref.ClockTenths.NEVER, trans.never.txt()),
      (Pref.ClockTenths.LOWTIME, trans.preferences.whenTimeRemainingLessThanTenSeconds.txt()),
      (Pref.ClockTenths.ALWAYS, trans.always.txt())
    )

  def translatedMoveEventChoices(using Lang) =
    List(
      (Pref.MoveEvent.CLICK, trans.preferences.clickTwoSquares.txt()),
      (Pref.MoveEvent.DRAG, trans.preferences.dragPiece.txt()),
      (Pref.MoveEvent.BOTH, trans.preferences.bothClicksAndDrag.txt())
    )

  def translatedTakebackChoices(using Lang) =
    List(
      (Pref.Takeback.NEVER, trans.never.txt()),
      (Pref.Takeback.ALWAYS, trans.always.txt()),
      (Pref.Takeback.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def translatedMoretimeChoices(using Lang) =
    List(
      (Pref.Moretime.NEVER, trans.never.txt()),
      (Pref.Moretime.ALWAYS, trans.always.txt()),
      (Pref.Moretime.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def translatedAutoQueenChoices(using Lang) =
    List(
      (Pref.AutoQueen.NEVER, trans.never.txt()),
      (Pref.AutoQueen.PREMOVE, trans.preferences.whenPremoving.txt()),
      (Pref.AutoQueen.ALWAYS, trans.always.txt())
    )

  def translatedAutoThreefoldChoices(using Lang) =
    List(
      (Pref.AutoThreefold.NEVER, trans.never.txt()),
      (Pref.AutoThreefold.ALWAYS, trans.always.txt()),
      (Pref.AutoThreefold.TIME, trans.preferences.whenTimeRemainingLessThanThirtySeconds.txt())
    )

  def submitMoveChoices(using Lang) =
    List(
      (Pref.SubmitMove.UNLIMITED, trans.unlimited.txt()),
      (Pref.SubmitMove.CORRESPONDENCE, trans.correspondence.txt()),
      (Pref.SubmitMove.CLASSICAL, trans.classical.txt()),
      (Pref.SubmitMove.RAPID, trans.rapid.txt()),
      (Pref.SubmitMove.BLITZ, "Blitz")
    )

  def confirmResignChoices(using Lang) =
    List(
      (Pref.ConfirmResign.NO, trans.no.txt()),
      (Pref.ConfirmResign.YES, trans.yes.txt())
    )

  def translatedRookCastleChoices(using Lang) =
    List(
      (Pref.RookCastle.NO, trans.preferences.castleByMovingTwoSquares.txt()),
      (Pref.RookCastle.YES, trans.preferences.castleByMovingOntoTheRook.txt())
    )

  def translatedChallengeChoices(using Lang) =
    List(
      (Pref.Challenge.NEVER, trans.never.txt()),
      (
        Pref.Challenge.RATING,
        trans.ifRatingIsPlusMinusX.txt(lila.pref.Pref.Challenge.ratingThreshold)
      ),
      (Pref.Challenge.FRIEND, trans.onlyFriends.txt()),
      (Pref.Challenge.REGISTERED, trans.ifRegistered.txt()),
      (Pref.Challenge.ALWAYS, trans.always.txt())
    )

  def translatedMessageChoices(using Lang) =
    List(
      (Pref.Message.NEVER, trans.onlyExistingConversations.txt()),
      (Pref.Message.FRIEND, trans.onlyFriends.txt()),
      (Pref.Message.ALWAYS, trans.always.txt())
    )

  def translatedStudyInviteChoices(using Lang) = privacyBaseChoices
  def translatedPalantirChoices(using Lang)    = privacyBaseChoices
  private def privacyBaseChoices(using Lang) =
    List(
      (Pref.StudyInvite.NEVER, trans.never.txt()),
      (Pref.StudyInvite.FRIEND, trans.onlyFriends.txt()),
      (Pref.StudyInvite.ALWAYS, trans.always.txt())
    )

  def translatedInsightShareChoices(using Lang) =
    List(
      (Pref.InsightShare.NOBODY, trans.withNobody.txt()),
      (Pref.InsightShare.FRIENDS, trans.withFriends.txt()),
      (Pref.InsightShare.EVERYBODY, trans.withEverybody.txt())
    )

  def translatedBoardResizeHandleChoices(using Lang) =
    List(
      (Pref.ResizeHandle.NEVER, trans.never.txt()),
      (Pref.ResizeHandle.INITIAL, trans.preferences.onlyOnInitialPosition.txt()),
      (Pref.ResizeHandle.ALWAYS, trans.always.txt())
    )

  def translatedBlindfoldChoices(using Lang) =
    List(
      Pref.Blindfold.NO  -> trans.no.txt(),
      Pref.Blindfold.YES -> trans.yes.txt()
    )

  def translatedBooleanIntChoices(using Lang) =
    List(
      0 -> trans.no.txt(),
      1 -> trans.yes.txt()
    )

  def translatedBooleanChoices(using Lang) =
    List(
      false -> trans.no.txt(),
      true  -> trans.yes.txt()
    )
