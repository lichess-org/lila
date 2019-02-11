package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.pref.{ Pref, PrefCateg }

import controllers.routes

object pref {

  private def categFieldset(categ: lila.pref.PrefCateg, active: lila.pref.PrefCateg)(body: Frag) =
    div(cls := List("none" -> (categ != active)))(body)

  private def setting(name: Frag, body: Frag) = li(h2(name), body)

  private def radios(field: play.api.data.Field, options: Iterable[(Any, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"${field.id}_${v._1}"
        val checked = field.value has v._1.toString
        div(
          input(
            st.id := s"$prefix$id",
            st.checked := checked option true,
            cls := checked option "active",
            `type` := "radio",
            value := v._1.toString,
            name := field.name
          ),
          label(`for` := s"$prefix$id")(v._2)
        )
      } toList
    )

  def apply(u: lila.user.User, form: play.api.data.Form[_], categ: lila.pref.PrefCateg)(implicit ctx: Context) = account.layout(
    title = s"${bits.categName(categ)} - ${u.username} - ${trans.preferences.txt()}",
    active = categ.slug,
    evenMoreCss = cssTag("pref.css")
  ) {
      val booleanChoices = Seq(0 -> trans.no.txt(), 1 -> trans.yes.txt())
      div(cls := "content_box small_box prefs")(
        div(cls := "signup_box")(
          h1(cls := "lichess_title text", dataIcon := "%")(bits.categName(categ)),
          st.form(cls := "autosubmit", action := routes.Pref.formApply, method := "POST")(
            categFieldset(PrefCateg.GameDisplay, categ) {
              ul(
                setting(
                  trans.pieceAnimation.frag(),
                  radios(form("display.animation"), translatedAnimationChoices)
                ),
                setting(
                  trans.materialDifference.frag(),
                  radios(form("display.captured"), booleanChoices)
                ),
                setting(
                  trans.boardHighlights.frag(),
                  radios(form("display.highlight"), booleanChoices)
                ),
                setting(
                  trans.pieceDestinations.frag(),
                  radios(form("display.destination"), booleanChoices)
                ),
                setting(
                  trans.boardCoordinates.frag(),
                  radios(form("display.coords"), translatedBoardCoordinateChoices)
                ),
                setting(
                  trans.moveListWhilePlaying.frag(),
                  radios(form("display.replay"), translatedMoveListWhilePlayingChoices)
                ),
                setting(
                  trans.pgnPieceNotation.frag(),
                  radios(form("display.pieceNotation"), translatedPieceNotationChoices)
                ),
                setting(
                  trans.zenMode.frag(),
                  radios(form("display.zen"), booleanChoices)
                ),
                setting(
                  trans.blindfoldChess.frag(),
                  radios(form("display.blindfold"), translatedBlindfoldChoices)
                )
              )
            },
            categFieldset(PrefCateg.ChessClock, categ) {
              ul(
                setting(
                  trans.tenthsOfSeconds.frag(),
                  radios(form("clockTenths"), translatedClockTenthsChoices)
                ),
                setting(
                  trans.horizontalGreenProgressBars.frag(),
                  radios(form("clockBar"), booleanChoices)
                ),
                setting(
                  trans.soundWhenTimeGetsCritical.frag(),
                  radios(form("clockSound"), booleanChoices)
                )
              )
            },
            categFieldset(PrefCateg.GameBehavior, categ) {
              ul(
                setting(
                  trans.howDoYouMovePieces.frag(),
                  radios(form("behavior.moveEvent"), translatedMoveEventChoices)
                ),
                setting(
                  trans.premovesPlayingDuringOpponentTurn.frag(),
                  radios(form("behavior.premove"), booleanChoices)
                ),
                setting(
                  trans.takebacksWithOpponentApproval.frag(),
                  radios(form("behavior.takeback"), translatedTakebackChoices)
                ),
                setting(
                  trans.promoteToQueenAutomatically.frag(),
                  radios(form("behavior.autoQueen"), translatedAutoQueenChoices)
                ),
                setting(
                  trans.claimDrawOnThreefoldRepetitionAutomatically.frag(),
                  radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices)
                ),
                setting(
                  trans.moveConfirmation.frag(),
                  radios(form("behavior.submitMove"), submitMoveChoices)
                ),
                setting(
                  trans.confirmResignationAndDrawOffers.frag(),
                  radios(form("behavior.confirmResign"), confirmResignChoices)
                ),
                setting(
                  trans.inputMovesWithTheKeyboard.frag(),
                  radios(form("behavior.keyboardMove"), booleanChoices)
                ),
                setting(
                  trans.castleByMovingTheKingTwoSquaresOrOntoTheRook.frag(),
                  radios(form("behavior.rookCastle"), translatedRookCastleChoices)
                )
              )
            },
            categFieldset(PrefCateg.Privacy, categ) {
              ul(
                setting(
                  trans.letOtherPlayersFollowYou.frag(),
                  radios(form("follow"), booleanChoices)
                ),
                setting(
                  trans.letOtherPlayersChallengeYou.frag(),
                  radios(form("challenge"), translatedChallengeChoices)
                ),
                setting(
                  trans.letOtherPlayersMessageYou.frag(),
                  radios(form("message"), translatedMessageChoices)
                ),
                setting(
                  trans.letOtherPlayersInviteYouToStudy.frag(),
                  radios(form("studyInvite"), translatedStudyInviteChoices)
                ),
                setting(
                  trans.shareYourInsightsData.frag(),
                  radios(form("insightShare"), translatedInsightSquareChoices)
                )
              )
            },
            p(cls := "saved text none", dataIcon := "E")(trans.yourPreferencesHaveBeenSaved.frag())
          )
        )
      )
    }
}
