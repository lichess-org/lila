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

  private def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  def apply(u: lila.user.User, form: play.api.data.Form[?], categ: lila.pref.PrefCateg)(using PageContext) =
    account.layout(
      title = s"${bits.categName(categ)} - ${u.username} - ${preferences.txt()}",
      active = categ.slug
    ):
      val booleanChoices = translatedBooleanIntChoices
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(bits.categName(categ)),
        postForm(cls := "autosubmit", action := routes.Pref.formApply)(
          categFieldset(PrefCateg.Display, categ)(
            setting(
              pieceAnimation(),
              radios(form("display.animation"), translatedAnimationChoices)
            ),
            setting(
              materialDifference(),
              radios(form("display.captured"), booleanChoices)
            ),
            setting(
              boardHighlights(),
              radios(form("display.highlight"), booleanChoices)
            ),
            setting(
              pieceDestinations(),
              radios(form("display.destination"), booleanChoices)
            ),
            setting(
              boardCoordinates(),
              radios(form("display.coords"), translatedBoardCoordinateChoices)
            ),
            setting(
              moveListWhilePlaying(),
              radios(form("display.replay"), translatedMoveListWhilePlayingChoices)
            ),
            setting(
              pgnPieceNotation(),
              radios(form("display.pieceNotation"), translatedPieceNotationChoices)
            ),
            setting(
              zenMode(),
              radios(form("display.zen"), translatedZenChoices)
            ),
            setting(
              displayBoardResizeHandle(),
              radios(form("display.resizeHandle"), translatedBoardResizeHandleChoices)
            ),
            setting(
              blindfoldChess(),
              radios(form("display.blindfold"), translatedBlindfoldChoices)
            ),
            setting(
              showPlayerRatings(),
              frag(
                radios(form("ratings"), booleanChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(
                  explainShowPlayerRatings()
                )
              )
            )
          ),
          categFieldset(PrefCateg.ChessClock, categ)(
            setting(
              tenthsOfSeconds(),
              radios(form("clock.tenths"), translatedClockTenthsChoices)
            ),
            setting(
              horizontalGreenProgressBars(),
              radios(form("clock.bar"), booleanChoices)
            ),
            setting(
              soundWhenTimeGetsCritical(),
              radios(form("clock.sound"), booleanChoices)
            ),
            setting(
              giveMoreTime(),
              radios(form("clock.moretime"), translatedMoretimeChoices)
            )
          ),
          categFieldset(PrefCateg.GameBehavior, categ)(
            setting(
              howDoYouMovePieces(),
              radios(form("behavior.moveEvent"), translatedMoveEventChoices)
            ),
            setting(
              premovesPlayingDuringOpponentTurn(),
              radios(form("behavior.premove"), booleanChoices)
            ),
            setting(
              takebacksWithOpponentApproval(),
              radios(form("behavior.takeback"), translatedTakebackChoices)
            ),
            setting(
              promoteToQueenAutomatically(),
              frag(
                radios(form("behavior.autoQueen"), translatedAutoQueenChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(
                  explainPromoteToQueenAutomatically()
                )
              )
            ),
            setting(
              claimDrawOnThreefoldRepetitionAutomatically(),
              radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices)
            ),
            setting(
              moveConfirmation(),
              frag(
                bitCheckboxes(form("behavior.submitMove"), submitMoveChoices),
                div(cls := "help text shy", dataIcon := licon.InfoCircle)(
                  "Multiple choices. ",
                  explainCanThenBeTemporarilyDisabled()
                )
              )
            ),
            setting(
              confirmResignationAndDrawOffers(),
              radios(form("behavior.confirmResign"), confirmResignChoices)
            ),
            setting(
              castleByMovingTheKingTwoSquaresOrOntoTheRook(),
              radios(form("behavior.rookCastle"), translatedRookCastleChoices)
            ),
            setting(
              inputMovesWithTheKeyboard(),
              radios(form("behavior.keyboardMove"), booleanChoices)
            ),
            setting(
              inputMovesWithVoice(),
              radios(form("behavior.voice"), booleanChoices)
            ),
            setting(
              snapArrowsToValidMoves(),
              radios(form("behavior.arrowSnap"), booleanChoices)
            )(cls := "arrow-snap"),
            setting(
              sayGgWpAfterLosingOrDrawing(),
              radios(form("behavior.courtesy"), booleanChoices)
            ),
            setting(
              scrollOnTheBoardToReplayMoves(),
              radios(form("behavior.scrollMoves"), booleanChoices)
            )
          ),
          categFieldset(PrefCateg.Privacy, categ)(
            setting(
              trans.letOtherPlayersFollowYou(),
              radios(form("follow"), booleanChoices)
            ),
            setting(
              trans.letOtherPlayersChallengeYou(),
              radios(form("challenge"), translatedChallengeChoices)
            ),
            setting(
              trans.letOtherPlayersMessageYou(),
              radios(form("message"), translatedMessageChoices)
            ),
            setting(
              trans.letOtherPlayersInviteYouToStudy(),
              radios(form("studyInvite"), translatedStudyInviteChoices)
            ),
            setting(
              trans.shareYourInsightsData(),
              radios(form("insightShare"), translatedInsightShareChoices)
            )
          ),
          p(cls := "saved text none", dataIcon := licon.Checkmark)(yourPreferencesHaveBeenSaved())
        )
      )
