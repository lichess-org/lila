package lidraughts.app
package templating

import draughts.{ Mode, Speed }
import lidraughts.api.Context
import lidraughts.i18n.{ I18nKeys => trans }
import lidraughts.pref.Pref
import lidraughts.report.Reason
import lidraughts.setup.TimeMode
import lidraughts.tournament.System

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

  def translatedModeChoicesById(implicit ctx: Context) = List(
    (Mode.Casual.id, trans.casual.txt()),
    (Mode.Rated.id, trans.rated.txt())
  )

  def translatedModeChoicesTournament(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casualTournament.txt(), none),
    (Mode.Rated.id.toString, trans.ratedTournament.txt(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena"
  )

  def translatedColorChoices(implicit ctx: Context) = List(
    "white" -> trans.white.txt(),
    "random" -> trans.randomColor.txt(),
    "black" -> trans.black.txt()
  )

  def translatedChatChoices(implicit ctx: Context) = List(
    "everyone" -> trans.everyone.txt(),
    "spectators" -> trans.spectatorsOnly.txt(),
    "participants" -> trans.participantsOnly.txt()
  )

  def translatedHasAiChoices(implicit ctx: Context) = List(
    0 -> trans.human.txt(),
    1 -> trans.computer.txt()
  )

  def translatedWinnerColorChoices(implicit ctx: Context) = List(
    1 -> trans.white.txt(),
    2 -> trans.black.txt(),
    3 -> trans.none.txt()
  )

  def translatedSortFieldChoices(implicit ctx: Context) = List(
    lidraughts.gameSearch.Sorting.fields(0)._1 -> trans.date.txt(),
    lidraughts.gameSearch.Sorting.fields(1)._1 -> trans.numberOfTurns.txt(),
    lidraughts.gameSearch.Sorting.fields(2)._1 -> trans.averageElo.txt()
  )

  def translatedSortOrderChoices(implicit ctx: Context) = List(
    "desc" -> trans.descending.txt(),
    "asc" -> trans.ascending.txt()
  )

  def translatedAverageRatingChoices(implicit ctx: Context) =
    lidraughts.gameSearch.Query.averageRatings.map { r => r._1 -> s"${r._1} ${trans.rating.txt()}" }

  def translatedTurnsChoices(implicit ctx: Context) = ((1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25)) map { d =>
    d -> trans.nbTurns.pluralSameTxt(d)
  } toList

  def translatedDurationChoices(implicit ctx: Context) = {
    ((30, trans.nbSeconds.pluralSameTxt(30)) ::
      (List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30) map { d =>
        d -> trans.nbMinutes.pluralSameTxt(d / 60)
      })) :+
      (60 * 60 * 1, trans.nbHours.pluralSameTxt(1)) :+
      (60 * 60 * 2, trans.nbHours.pluralSameTxt(2)) :+
      (60 * 60 * 3, trans.nbHours.pluralSameTxt(3))
  }

  def translatedClockInitChoices(implicit ctx: Context) = List(
    (0, trans.nbSeconds.pluralSameTxt(0)),
    (30, trans.nbSeconds.pluralSameTxt(30)),
    (45, trans.nbSeconds.pluralSameTxt(45))
  ) ::: (List(60 * 1, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30, 60 * 45, 60 * 60, 60 * 90, 60 * 120, 60 * 150, 60 * 180) map { d =>
      d -> trans.nbMinutes.pluralSameTxt(d / 60)
    })

  def translatedClockIncChoices(implicit ctx: Context) = List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180) map { d =>
    d -> trans.nbSeconds.pluralSameTxt(d)
  }

  private def variantTuple(variant: draughts.variant.Variant)(implicit ctx: Context): SelectChoice =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (draughts.variant.Standard.id.toString, trans.standard.txt(), draughts.variant.Standard.title.some)
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
      variantTuple(draughts.variant.Frisian) :+
      variantTuple(draughts.variant.Frysk) :+
      variantTuple(draughts.variant.Antidraughts) :+
      variantTuple(draughts.variant.Breakthrough) :+
      variantTuple(draughts.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(draughts.variant.FromPosition)

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

  def translatedGameResultNotationChoices(implicit ctx: Context) =
    Pref.GameResult.choices.toList

  def translatedClockTenthsChoices(implicit ctx: Context) = List(
    (Pref.ClockTenths.NEVER, trans.never.txt()),
    (Pref.ClockTenths.LOWTIME, trans.whenTimeRemainingLessThanTenSeconds.txt()),
    (Pref.ClockTenths.ALWAYS, trans.always.txt())
  )

  def translatedFullCaptureChoices(implicit ctx: Context) = List(
    (Pref.FullCapture.NO, trans.stepByStep.txt()),
    (Pref.FullCapture.YES, trans.allAtOnce.txt())
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

  def translatedMoretimeChoices(implicit ctx: Context) = List(
    (Pref.Moretime.NEVER, trans.never.txt()),
    (Pref.Moretime.ALWAYS, trans.always.txt()),
    (Pref.Moretime.CASUAL, trans.inCasualGamesOnly.txt())
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

  def translatedChallengeChoices(implicit ctx: Context) = List(
    (Pref.Challenge.NEVER, trans.never.txt()),
    (Pref.Challenge.RATING, trans.ifRatingIsPlusMinusX.txt(lidraughts.pref.Pref.Challenge.ratingThreshold)),
    (Pref.Challenge.FRIEND, trans.onlyFriends.txt()),
    (Pref.Challenge.ALWAYS, trans.always.txt())
  )

  def translatedMessageChoices(implicit ctx: Context) = privacyBaseChoices
  def translatedStudyInviteChoices(implicit ctx: Context) = privacyBaseChoices
  def translatedPalantirChoices(implicit ctx: Context) = privacyBaseChoices

  def privacyBaseChoices(implicit ctx: Context) = List(
    (Pref.StudyInvite.NEVER, trans.never.txt()),
    (Pref.StudyInvite.FRIEND, trans.onlyFriends.txt()),
    (Pref.StudyInvite.ALWAYS, trans.always.txt())
  )

  def translatedInsightShareChoices(implicit ctx: Context) = List(
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
