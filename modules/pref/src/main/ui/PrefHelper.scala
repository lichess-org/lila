package lila
package pref

import lila.core.i18n.{ I18nKey as trans, Translate }

trait PrefHelper:

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

  def translatedRatingsChoices(using Translate) =
    List(
      (Pref.Ratings.NO, trans.site.no.txt()),
      (Pref.Ratings.YES, trans.site.yes.txt()),
      (Pref.Ratings.EXCEPT_GAME, trans.preferences.exceptInGame.txt())
    )

  def translatedBoardCoordinateChoices(using Translate) =
    List(
      (Pref.Coords.NONE, trans.site.no.txt()),
      (Pref.Coords.INSIDE, trans.site.insideTheBoard.txt()),
      (Pref.Coords.OUTSIDE, trans.site.outsideTheBoard.txt()),
      (Pref.Coords.ALL, trans.site.allSquaresOfTheBoard.txt())
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
        trans.site.ifRatingIsPlusMinusX.txt(Pref.Challenge.ratingThreshold)
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
