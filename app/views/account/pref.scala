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

  private def setting(name: Frag, body: Frag, display: Boolean = true) = {
    if(display) st.section(h2(name), body) else st.section(style:="display:none;")(h2(name), body)
  }

  private def radios(field: play.api.data.Field, options: Iterable[(Any, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { v =>
        val id      = s"${field.id}_${v._1}"
        val checked = field.value has v._1.toString
        div(
          input(
            st.id := s"$prefix$id",
            checked option st.checked,
            cls := checked option "active",
            `type` := "radio",
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
        postForm(cls := "autosubmit", action := routes.Pref.formApply())(
          categFieldset(PrefCateg.GameDisplay, categ)(
            setting(
              pieceAnimation(),
              radios(form("display.animation"), translatedAnimationChoices),
            ),
            setting(
              materialDifference(),
              radios(form("display.captured"), booleanChoices),
              false
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
              radios(form("display.pieceNotation"), translatedPieceNotationChoices),
              false
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
              radios(form("behavior.autoQueen"), translatedAutoQueenChoices),
              false
            ),
            setting(
              claimDrawOnThreefoldRepetitionAutomatically(),
              radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices),
              false
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
              radios(form("behavior.rookCastle"), translatedRookCastleChoices),
              false
            ),
            setting(
              inputMovesWithTheKeyboard(),
              radios(form("behavior.keyboardMove"), booleanChoices),
              false
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
              radios(form("insightShare"), translatedInsightShareChoices),
              false
            )
          ),
          p(cls := "saved text none", dataIcon := "E")(yourPreferencesHaveBeenSaved())
        )
      )
    }
}
