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

  def translatedModeChoicesById(implicit ctx: Context) = List(
    (Mode.Casual.id, I18nKeys.casual.txt()),
    (Mode.Rated.id, I18nKeys.rated.txt())
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

  def translatedHasAiChoices(implicit ctx: Context) = List(
    0 -> I18nKeys.human.txt(),
    1 -> I18nKeys.computer.txt()
  )

  def translatedWinnerColorChoices(implicit ctx: Context) = List(
    1 -> I18nKeys.white.txt(),
    2 -> I18nKeys.black.txt(),
    3 -> I18nKeys.none.txt()
  )

  def translatedSortFieldChoices(implicit ctx: Context) = List(
    lidraughts.gameSearch.Sorting.fields(0)._1 -> I18nKeys.date.txt(),
    lidraughts.gameSearch.Sorting.fields(1)._1 -> I18nKeys.numberOfTurns.txt(),
    lidraughts.gameSearch.Sorting.fields(2)._1 -> I18nKeys.averageElo.txt()
  )

  def translatedSortOrderChoices(implicit ctx: Context) = List(
    "desc" -> I18nKeys.descending.txt(),
    "asc" -> I18nKeys.ascending.txt()
  )

  def translatedAverageRatingChoices(implicit ctx: Context) =
    lidraughts.gameSearch.Query.averageRatings.map { r => r._1 -> s"${r._1} ${I18nKeys.rating.txt()}" }

  def translatedTurnsChoices(implicit ctx: Context) = ((1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25)) map { d =>
    d -> I18nKeys.nbTurns.pluralSameTxt(d)
  } toList

  def translatedDurationChoices(implicit ctx: Context) = {
    ((30, I18nKeys.nbSeconds.pluralSameTxt(30)) ::
      (List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30) map { d =>
        d -> I18nKeys.nbMinutes.pluralSameTxt(d / 60)
      })) :+
      (60 * 60 * 1, I18nKeys.nbHours.pluralSameTxt(1)) :+
      (60 * 60 * 2, I18nKeys.nbHours.pluralSameTxt(2)) :+
      (60 * 60 * 3, I18nKeys.nbHours.pluralSameTxt(3))
  }

  def translatedClockInitChoices(implicit ctx: Context) = List(
    (0, I18nKeys.nbSeconds.pluralSameTxt(0)),
    (30, I18nKeys.nbSeconds.pluralSameTxt(30)),
    (45, I18nKeys.nbSeconds.pluralSameTxt(45))
  ) ::: (List(60 * 1, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30, 60 * 45, 60 * 60, 60 * 90, 60 * 120, 60 * 150, 60 * 180) map { d =>
      d -> I18nKeys.nbMinutes.pluralSameTxt(d / 60)
    })

  def translatedClockIncChoices(implicit ctx: Context) = List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180) map { d =>
    d -> I18nKeys.nbSeconds.pluralSameTxt(d)
  }

  private def variantTuple(variant: draughts.variant.Variant)(implicit ctx: Context): SelectChoice =
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

  def translatedFullCaptureChoices(implicit ctx: Context) = List(
    (Pref.FullCapture.NO, I18nKeys.stepByStep.txt()),
    (Pref.FullCapture.YES, I18nKeys.allAtOnce.txt())
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
