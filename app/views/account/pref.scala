package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.pref.PrefCateg

import controllers.routes

object pref:
  import bits.*
  import trans.preferences.*

  private def categFieldset(categ: lila.pref.PrefCateg, active: lila.pref.PrefCateg) =
    div(cls := List("none" -> (categ != active)))

  private def setting(name: Frag, body: Frag, settingId: String) =
    st.section(a(href := "#" + settingId)(h2(id := settingId)(name)), body)

  def apply(u: lila.user.User, form: play.api.data.Form[?], categ: lila.pref.PrefCateg)(using PageContext) =
    account.layout(
      title = s"${bits.categName(categ)} - ${u.username} - ${preferences.txt()}",
      active = categ.slug
    ):
      val booleanChoices = translatedBooleanIntChoices
      div(cls := "box box-pad")(
        h1(cls := "box__top")(bits.categName(categ)),
        postForm(cls := "autosubmit", action := routes.Pref.formApply)(
          categFieldset(PrefCateg.Display, categ)(
            setting(
              pieceAnimation(),
              radios(form("display.animation"), translatedAnimationChoices),
              "pieceAnimation"
            ),
            setting(
              materialDifference(),
              radios(form("display.captured"), booleanChoices),
              "materialDifference"
            ),
            setting(
              boardHighlights(),
              radios(form("display.highlight"), booleanChoices),
              "boardHighlights"
            ),
            setting(
              pieceDestinations(),
              radios(form("display.destination"), booleanChoices),
              "pieceDestinations"
            ),
            setting(
              boardCoordinates(),
              radios(form("display.coords"), translatedBoardCoordinateChoices),
              "boardCoordinates"
            ),
            setting(
              moveListWhilePlaying(),
              radios(form("display.replay"), translatedMoveListWhilePlayingChoices),
              "moveListWhilePlaying"
            ),
            setting(
              pgnPieceNotation(),
              radios(form("display.pieceNotation"), translatedPieceNotationChoices),
              "pgnPieceNotation"
            ),
            setting(
              zenMode(),
              radios(form("display.zen"), translatedZenChoices),
              "zenMode"
            ),
            setting(
              displayBoardResizeHandle(),
              radios(form("display.resizeHandle"), translatedBoardResizeHandleChoices),
              "displayBoardResizeHandle"
            ),
            setting(
              blindfoldChess(),
              radios(form("display.blindfold"), translatedBlindfoldChoices),
              "blindfoldChess"
            ),
            setting(
              showPlayerRatings(),
              frag(
                radios(form("ratings"), booleanChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(explainShowPlayerRatings())
              ),
              "showRatings"
            ),
            setting(
              showFlairs(),
              radios(form("flairs"), translatedBooleanChoices),
              "showFlairs"
            )
          ),
          categFieldset(PrefCateg.ChessClock, categ)(
            setting(
              tenthsOfSeconds(),
              radios(form("clock.tenths"), translatedClockTenthsChoices),
              "tenthsOfSeconds"
            ),
            setting(
              horizontalGreenProgressBars(),
              radios(form("clock.bar"), booleanChoices),
              "horizontalGreenProgressBars"
            ),
            setting(
              soundWhenTimeGetsCritical(),
              radios(form("clock.sound"), booleanChoices),
              "soundWhenTimeGetsCritical"
            ),
            setting(
              giveMoreTime(),
              radios(form("clock.moretime"), translatedMoretimeChoices),
              "giveMoreTime"
            )
          ),
          categFieldset(PrefCateg.GameBehavior, categ)(
            setting(
              howDoYouMovePieces(),
              radios(form("behavior.moveEvent"), translatedMoveEventChoices),
              "howDoYouMovePieces"
            ),
            setting(
              premovesPlayingDuringOpponentTurn(),
              radios(form("behavior.premove"), booleanChoices),
              "premovesPlayingDuringOpponentTurn"
            ),
            setting(
              takebacksWithOpponentApproval(),
              radios(form("behavior.takeback"), translatedTakebackChoices),
              "takebacksWithOpponentApproval"
            ),
            setting(
              promoteToQueenAutomatically(),
              frag(
                radios(form("behavior.autoQueen"), translatedAutoQueenChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(
                  explainPromoteToQueenAutomatically()
                )
              ),
              "promoteToQueenAutomatically"
            ),
            setting(
              claimDrawOnThreefoldRepetitionAutomatically(),
              radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices),
              "claimDrawOnThreefoldRepetitionAutomatically"
            ),
            setting(
              moveConfirmation(),
              frag(
                bitCheckboxes(form("behavior.submitMove"), submitMoveChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(
                  "Multiple choices. ",
                  explainCanThenBeTemporarilyDisabled()
                )
              ),
              "moveConfirmation"
            ),
            setting(
              confirmResignationAndDrawOffers(),
              radios(form("behavior.confirmResign"), confirmResignChoices),
              "confirmResignationAndDrawOffers"
            ),
            setting(
              castleByMovingTheKingTwoSquaresOrOntoTheRook(),
              radios(form("behavior.rookCastle"), translatedRookCastleChoices),
              "castleByMovingTheKingTwoSquaresOrOntoTheRook"
            ),
            setting(
              inputMovesWithTheKeyboard(),
              radios(form("behavior.keyboardMove"), booleanChoices),
              "inputMovesWithTheKeyboard"
            ),
            setting(
              inputMovesWithVoice(),
              radios(form("behavior.voice"), booleanChoices),
              "inputMovesWithVoice"
            ),
            setting(
              snapArrowsToValidMoves(),
              radios(form("behavior.arrowSnap"), booleanChoices),
              "snapArrowsToValidMoves"
            )(cls := "arrow-snap"),
            setting(
              sayGgWpAfterLosingOrDrawing(),
              radios(form("behavior.courtesy"), booleanChoices),
              "sayGgWpAfterLosingOrDrawing"
            ),
            setting(
              scrollOnTheBoardToReplayMoves(),
              radios(form("behavior.scrollMoves"), booleanChoices),
              "scrollOnTheBoardToReplayMoves"
            )
          ),
          categFieldset(PrefCateg.Privacy, categ)(
            setting(
              trans.letOtherPlayersFollowYou(),
              radios(form("follow"), booleanChoices),
              "letOtherPlayersFollowYou"
            ),
            setting(
              trans.letOtherPlayersChallengeYou(),
              radios(form("challenge"), translatedChallengeChoices),
              "letOtherPlayersChallengeYou"
            ),
            setting(
              trans.letOtherPlayersMessageYou(),
              radios(form("message"), translatedMessageChoices),
              "letOtherPlayersMessageYou"
            ),
            setting(
              trans.letOtherPlayersInviteYouToStudy(),
              radios(form("studyInvite"), translatedStudyInviteChoices),
              "letOtherPlayersInviteYouToStudy"
            ),
            setting(
              trans.shareYourInsightsData(),
              radios(form("insightShare"), translatedInsightShareChoices),
              "shareYourInsightsData"
            )
          ),
          p(cls := "saved text none", dataIcon := licon.Checkmark)(yourPreferencesHaveBeenSaved())
        )
      )
