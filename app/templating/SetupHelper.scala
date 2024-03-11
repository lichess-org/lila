package lila.app
package templating

import shogi.{ Mode, Speed }
import shogi.variant.Variant
import play.api.i18n.Lang

import lila.i18n.{ I18nKeys => trans }
import lila.pref.Pref
import lila.report.Reason
import lila.setup.TimeMode

trait SetupHelper { self: I18nHelper =>

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
  ).map { v =>
    (v.toString, v.toString, none)
  }

  val clockIncrementChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  } map { s =>
    (s.toString, s.toString, none)
  }

  val clockByoyomiChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  } map { s =>
    (s.toString, s.toString, none)
  }

  val periodsChoices: List[SelectChoice] = {
    (1 to 5).toList map { s =>
      (s.toString, s.toString, none)
    }
  }

  def translatedCorresDaysChoices(implicit lang: Lang) =
    List(1, 2, 3, 5, 7, 10, 14) map { d =>
      (d.toString, trans.nbDays.pluralSameTxt(d), none)
    }

  def anonTranslatedTimeModeChoices(implicit lang: Lang) =
    List(
      (TimeMode.RealTime.id.toString, trans.realTime.txt(), none)
    )

  def translatedTimeModeChoices(implicit lang: Lang) = {
    List(
      (TimeMode.RealTime.id.toString, trans.realTime.txt(), none),
      (TimeMode.Correspondence.id.toString, trans.correspondence.txt(), none)
      // (TimeMode.Unlimited.id.toString, trans.unlimited.txt(), none)
    )
  }

  def translatedReasonChoices(implicit lang: Lang) =
    List(
      (Reason.Cheat.key, trans.cheat.txt()),
      (Reason.Comm.key, trans.insult.txt()),
      (Reason.Comm.key, trans.troll.txt()),
      (Reason.Other.key, trans.other.txt())
    )

  def translatedModeChoices(implicit lang: Lang) =
    List(
      (Mode.Casual.id.toString, trans.casual.txt(), none),
      (Mode.Rated.id.toString, trans.rated.txt(), none)
    )

  def translatedBooleanFilterChoices(implicit lang: Lang) =
    List(
      (1, trans.yes.txt(), none),
      (0, trans.no.txt(), none)
    )

  def translatedBooleanYesFilterChoice(implicit lang: Lang) =
    List(
      (1, trans.yes.txt(), none)
    )

  def translatedModeChoicesTournament(implicit lang: Lang) =
    List(
      (Mode.Casual.id.toString, trans.casualTournament.txt(), none),
      (Mode.Rated.id.toString, trans.ratedTournament.txt(), none)
    )

  private val encodeId = (v: Variant) => v.id.toString

  private def variantTuple(encode: Variant => String)(variant: Variant)(implicit lang: Lang) =
    (encode(variant), transKeyTxt(variant.key), transKeyTxt(s"${variant.key}Description").some)

  def standardChoice(implicit lang: Lang): SelectChoice =
    standardChoice(encodeId)

  def standardChoice(encode: Variant => String)(implicit lang: Lang): SelectChoice =
    (encode(shogi.variant.Standard), trans.standard.txt(), trans.standardDescription.txt().some)

  def translatedVariantChoices(implicit lang: Lang): List[SelectChoice] =
    standardChoice ::
      List(
        shogi.variant.Minishogi,
        shogi.variant.Chushogi,
        shogi.variant.Annanshogi,
        shogi.variant.Kyotoshogi,
        shogi.variant.Checkshogi
      ).map(variantTuple(encodeId))

  def translatedVariantChoices(
      encode: Variant => String
  )(implicit lang: Lang): List[SelectChoice] =
    standardChoice(encode) :: List(
      shogi.variant.Minishogi,
      shogi.variant.Chushogi,
      shogi.variant.Annanshogi,
      shogi.variant.Kyotoshogi,
      shogi.variant.Checkshogi
    ).map(variantTuple(encode))

  def translatedAiChoices(implicit lang: Lang) =
    standardChoice :: List(
      shogi.variant.Minishogi,
      shogi.variant.Kyotoshogi
    ).map(variantTuple(encodeId))

  def translatedSpeedChoices(implicit lang: Lang) =
    Speed.limited map { s =>
      val minutes = s.range.max / 60 + 1
      (
        s.id.toString,
        s.toString + " - " + trans.lessThanNbMinutes.pluralSameTxt(minutes),
        none
      )
    }

  def translatedBoardLayoutChoices(implicit lang: Lang) =
    List(
      (Pref.BoardLayout.DEFAULT, trans.default.txt()),
      (Pref.BoardLayout.COMPACT, trans.compact.txt()),
      (Pref.BoardLayout.SMALL, trans.preferences.smallMoves.txt())
    )

  def translatedAnimationChoices(implicit lang: Lang) =
    List(
      (Pref.Animation.NONE, trans.none.txt()),
      (Pref.Animation.FAST, trans.fast.txt()),
      (Pref.Animation.NORMAL, trans.normal.txt()),
      (Pref.Animation.SLOW, trans.slow.txt())
    )

  def translatedBoardCoordinateChoices(implicit lang: Lang) =
    List(
      (Pref.Coords.NONE, trans.no.txt()),
      (Pref.Coords.INSIDE, trans.insideTheBoard.txt()),
      (Pref.Coords.OUTSIDE, trans.outsideTheBoard.txt()),
      (Pref.Coords.EDGE, trans.edgeOfTheBoard.txt())
    )

  def translatedMoveListWhilePlayingChoices(implicit lang: Lang) =
    List(
      (Pref.Replay.NEVER, trans.never.txt()),
      (Pref.Replay.SLOW, trans.onSlowGames.txt()),
      (Pref.Replay.ALWAYS, trans.always.txt())
    )

  def translatedColorNameChoices(implicit lang: Lang) =
    List(
      (Pref.ColorName.LANG, s"${trans.language.txt()} - (${trans.sente.txt()}/${trans.gote.txt()})"),
      (Pref.ColorName.SENTEJP, "先手/後手"),
      (Pref.ColorName.SENTE, "Sente/Gote"),
      (Pref.ColorName.BLACK, s"${trans.black.txt()}/${trans.white.txt()}")
    )

  def translatedClockTenthsChoices(implicit lang: Lang) =
    List(
      (Pref.ClockTenths.NEVER, trans.never.txt()),
      (Pref.ClockTenths.LOWTIME, trans.preferences.whenTimeRemainingLessThanTenSeconds.txt()),
      (Pref.ClockTenths.ALWAYS, trans.always.txt())
    )

  def translatedClockCountdownChoices(implicit lang: Lang) =
    List(
      (Pref.ClockCountdown.NEVER, trans.never.txt()),
      (Pref.ClockCountdown.THREE, trans.nbSeconds.pluralSameTxt(3)),
      (Pref.ClockCountdown.FIVE, trans.nbSeconds.pluralSameTxt(5)),
      (Pref.ClockCountdown.TEN, trans.nbSeconds.pluralSameTxt(10))
    )

  def translatedMoveEventChoices(implicit lang: Lang) =
    List(
      (Pref.MoveEvent.CLICK, trans.preferences.clickTwoSquares.txt()),
      (Pref.MoveEvent.DRAG, trans.preferences.dragPiece.txt()),
      (Pref.MoveEvent.BOTH, trans.preferences.bothClicksAndDrag.txt())
    )

  def translatedTakebackChoices(implicit lang: Lang) =
    List(
      (Pref.Takeback.NEVER, trans.never.txt()),
      (Pref.Takeback.ALWAYS, trans.always.txt()),
      (Pref.Takeback.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def translatedMoretimeChoices(implicit lang: Lang) =
    List(
      (Pref.Moretime.NEVER, trans.never.txt()),
      (Pref.Moretime.ALWAYS, trans.always.txt()),
      (Pref.Moretime.CASUAL, trans.preferences.inCasualGamesOnly.txt())
    )

  def submitMoveChoices(implicit lang: Lang) =
    List(
      (Pref.SubmitMove.NEVER, trans.never.txt()),
      (Pref.SubmitMove.CORRESPONDENCE_ONLY, trans.preferences.inCorrespondenceGames.txt()),
      (Pref.SubmitMove.CORRESPONDENCE_UNLIMITED, trans.preferences.correspondenceAndUnlimited.txt()),
      (Pref.SubmitMove.ALWAYS, trans.always.txt())
    )

  def confirmResignChoices(implicit lang: Lang) =
    List(
      (Pref.ConfirmResign.NO, trans.no.txt()),
      (Pref.ConfirmResign.YES, trans.yes.txt())
    )

  def translatedChallengeChoices(implicit lang: Lang) =
    List(
      (Pref.Challenge.NEVER, trans.never.txt()),
      (
        Pref.Challenge.RATING,
        trans.ifRatingIsPlusMinusX.txt(lila.pref.Pref.Challenge.ratingThreshold)
      ),
      (Pref.Challenge.FRIEND, trans.onlyFriends.txt()),
      (Pref.Challenge.ALWAYS, trans.always.txt())
    )

  def translatedMessageChoices(implicit lang: Lang)     = privacyBaseChoices
  def translatedStudyInviteChoices(implicit lang: Lang) = privacyBaseChoices
  def translatedPalantirChoices(implicit lang: Lang)    = privacyBaseChoices

  def privacyBaseChoices(implicit lang: Lang) =
    List(
      (Pref.StudyInvite.NEVER, trans.never.txt()),
      (Pref.StudyInvite.FRIEND, trans.onlyFriends.txt()),
      (Pref.StudyInvite.ALWAYS, trans.always.txt())
    )

  def translatedBoardResizeHandleChoices(implicit lang: Lang) =
    List(
      (Pref.ResizeHandle.NEVER, trans.never.txt()),
      (Pref.ResizeHandle.INITIAL, trans.preferences.onlyOnInitialPosition.txt()),
      (Pref.ResizeHandle.ALWAYS, trans.always.txt())
    )

  def translatedBlindfoldChoices(implicit lang: Lang) =
    List(
      Pref.Blindfold.NO  -> trans.no.txt(),
      Pref.Blindfold.YES -> trans.yes.txt()
    )
}
