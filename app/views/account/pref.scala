package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.PrefCateg

import controllers.routes

object pref {

  import trans.preferences._

  private def categFieldset(categ: lila.pref.PrefCateg, active: lila.pref.PrefCateg) =
    div(cls := List("none" -> (categ != active)))

  private def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  private def radios(field: play.api.data.Field, options: Iterable[(Any, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { v =>
        val id      = s"${field.id}_${v._1}"
        val checked = field.value has v._1.toString
        div(
          input(
            st.id := s"$prefix$id",
            checked option st.checked,
            tpe := "radio",
            value := v._1.toString,
            name := field.name
          ),
          label(`for` := s"$prefix$id")(v._2)
        )
      }.toList
    )

  def apply(u: lila.user.User, form: play.api.data.Form[_], categ: lila.pref.PrefCateg)(implicit
      ctx: Context
  ) =
    account.layout(
      title = s"${bits.categName(categ)} - ${u.username} - ${preferences.txt()}",
      active = categ.slug
    ) {
      val booleanChoices = Seq(0 -> trans.no.txt(), 1 -> trans.yes.txt())
      div(cls := "account box box-pad")(
        h1(bits.categName(categ)),
        postForm(cls := "autosubmit", action := routes.Pref.formApply)(
          categFieldset(PrefCateg.GameDisplay, categ)(
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
              radios(form("display.zen"), booleanChoices)
            ),
            setting(
              displayBoardResizeHandle(),
              radios(form("display.resizeHandle"), translatedBoardResizeHandleChoices)
            ),
            setting(
              blindfoldChess(),
              radios(form("display.blindfold"), translatedBlindfoldChoices)
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
              radios(form("behavior.autoQueen"), translatedAutoQueenChoices)
            ),
            setting(
              claimDrawOnThreefoldRepetitionAutomatically(),
              radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices)
            ),
            setting(
              moveConfirmation(),
              radios(form("behavior.submitMove"), submitMoveChoices)
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
              trans.receiveForumNotifications(),
              radios(form("mention"), booleanChoices)
            ),
            setting(
              trans.shareYourInsightsData(),
              radios(form("insightShare"), translatedInsightShareChoices)
            )
          ),
          p(cls := "saved text none", dataIcon := "")(yourPreferencesHaveBeenSaved())
        )
      )
    }
}
