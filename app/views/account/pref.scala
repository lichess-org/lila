package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.pref.{ Pref, PrefCateg }

import controllers.routes

object pref {

  private def categFieldset(categ: lidraughts.pref.PrefCateg, active: lidraughts.pref.PrefCateg) =
    div(cls := List("none" -> (categ != active)))

  private def setting(name: Frag, body: Frag) = st.section(h2(name), body)

  private def radios(field: play.api.data.Field, options: Iterable[(Any, String)], prefix: String = "ir") =
    st.group(cls := "radio")(
      options.map { v =>
        val id = s"${field.id}_${v._1}"
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
      } toList
    )

  def apply(u: lidraughts.user.User, form: play.api.data.Form[_], categ: lidraughts.pref.PrefCateg)(implicit ctx: Context) = account.layout(
    title = s"${bits.categName(categ)} - ${u.username} - ${trans.preferences.txt()}",
    active = categ.slug
  ) {
    val booleanChoices = Seq(0 -> trans.no.txt(), 1 -> trans.yes.txt())
    div(cls := "account box box-pad")(
      h1(bits.categName(categ)),
      postForm(cls := "autosubmit", action := routes.Pref.formApply)(
        categFieldset(PrefCateg.GameDisplay, categ)(
          setting(
            trans.pieceAnimation(),
            radios(form("display.animation"), translatedAnimationChoices)
          ),
          setting(
            trans.materialDifference(),
            radios(form("display.captured"), booleanChoices)
          ),
          setting(
            trans.showKingMoves(),
            radios(form("display.kingMoves"), booleanChoices)
          ),
          setting(
            trans.boardHighlights(),
            radios(form("display.highlight"), booleanChoices)
          ),
          setting(
            trans.pieceDestinations(),
            radios(form("display.destination"), booleanChoices)
          ),
          setting(
            trans.moveListWhilePlaying(),
            radios(form("display.replay"), translatedMoveListWhilePlayingChoices)
          ),
          setting(
            trans.boardCoordinates(),
            radios(form("display.coords"), translatedBoardCoordinateChoices)
          ),
          setting(
            trans.coordinateSystem8x8(),
            radios(form("display.coordSystem"), translatedCoordinateSystemChoices)
          ),
          setting(
            trans.notationGameResult(),
            radios(form("display.gameResult"), translatedGameResultNotationChoices)
          ),
          setting(
            trans.zenMode(),
            radios(form("display.zen"), booleanChoices)
          ),
          setting(
            trans.displayBoardResizeHandle(),
            radios(form("display.resizeHandle"), translatedBoardResizeHandleChoices)
          ),
          setting(
            trans.blindfoldDraughts(),
            radios(form("display.blindfold"), translatedBlindfoldChoices)
          )
        ),
        categFieldset(PrefCateg.DraughtsClock, categ)(
          setting(
            trans.tenthsOfSeconds(),
            radios(form("clock.tenths"), translatedClockTenthsChoices)
          ),
          setting(
            trans.horizontalGreenProgressBars(),
            radios(form("clock.bar"), booleanChoices)
          ),
          setting(
            trans.soundWhenTimeGetsCritical(),
            radios(form("clock.sound"), booleanChoices)
          ),
          setting(
            trans.giveMoreTime(),
            radios(form("clock.moretime"), translatedMoretimeChoices)
          )
        ),
        categFieldset(PrefCateg.GameBehavior, categ)(
          setting(
            trans.howDoYouMovePieces(),
            radios(form("behavior.moveEvent"), translatedMoveEventChoices)
          ),
          setting(
            trans.howDoYouPlayMultiCaptures(),
            radios(form("behavior.fullCapture"), translatedFullCaptureChoices)
          ),
          setting(
            trans.premovesPlayingDuringOpponentTurn(),
            radios(form("behavior.premove"), booleanChoices)
          ),
          setting(
            trans.takebacksWithOpponentApproval(),
            radios(form("behavior.takeback"), translatedTakebackChoices)
          ),
          setting(
            trans.claimDrawOnThreefoldRepetitionAutomatically(),
            radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices)
          ),
          setting(
            trans.moveConfirmation(),
            radios(form("behavior.submitMove"), submitMoveChoices)
          ),
          setting(
            trans.confirmResignationAndDrawOffers(),
            radios(form("behavior.confirmResign"), confirmResignChoices)
          ),
          setting(
            trans.inputMovesWithTheKeyboard(),
            radios(form("behavior.keyboardMove"), booleanChoices)
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
        p(cls := "saved text none", dataIcon := "E")(trans.yourPreferencesHaveBeenSaved())
      )
    )
  }
}
