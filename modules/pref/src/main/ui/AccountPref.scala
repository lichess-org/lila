package lila.pref
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AccountPref(helpers: Helpers, helper: PrefHelper, bits: AccountUi):
  import helpers.{ *, given }
  import helper.*
  import bits.*
  import trans.preferences as trp

  private def categFieldset(categ: lila.pref.PrefCateg, active: lila.pref.PrefCateg) =
    div(cls := List("none" -> (categ != active)))

  private def setting(name: Frag, body: Frag, settingId: String) =
    st.section(a(href := "#" + settingId)(h2(id := settingId)(name)), body)

  def apply(u: User, form: play.api.data.Form[?], categ: lila.pref.PrefCateg)(using Context) =
    AccountPage(
      s"${bits.categName(categ)} - ${u.username} - ${trans.preferences.preferences.txt()}",
      categ.slug
    ):
      val booleanChoices = translatedBooleanIntChoices
      div(cls := "box box-pad")(
        h1(cls := "box__top")(bits.categName(categ)),
        postForm(cls := "autosubmit", action := routes.Pref.formApply)(
          categFieldset(PrefCateg.Display, categ)(
            setting(
              trp.pieceAnimation(),
              radios(form("display.animation"), translatedAnimationChoices),
              "pieceAnimation"
            ),
            setting(
              trp.materialDifference(),
              radios(form("display.captured"), booleanChoices),
              "materialDifference"
            ),
            setting(
              trp.boardHighlights(),
              radios(form("display.highlight"), booleanChoices),
              "boardHighlights"
            ),
            setting(
              trp.pieceDestinations(),
              radios(form("display.destination"), booleanChoices),
              "pieceDestinations"
            ),
            setting(
              trp.boardCoordinates(),
              radios(form("display.coords"), translatedBoardCoordinateChoices),
              "boardCoordinates"
            ),
            setting(
              trp.moveListWhilePlaying(),
              radios(form("display.replay"), translatedMoveListWhilePlayingChoices),
              "moveListWhilePlaying"
            ),
            setting(
              trp.pgnPieceNotation(),
              radios(form("display.pieceNotation"), translatedPieceNotationChoices),
              "pgnPieceNotation"
            ),
            setting(
              trp.zenMode(),
              radios(form("display.zen"), translatedZenChoices),
              "zenMode"
            ),
            setting(
              trp.displayBoardResizeHandle(),
              radios(form("display.resizeHandle"), translatedBoardResizeHandleChoices),
              "displayBoardResizeHandle"
            ),
            setting(
              trp.showPlayerRatings(),
              frag(
                radios(form("ratings"), translatedRatingsChoices),
                div(cls := "help text shy", dataIcon := Icon.InfoCircle)(trp.explainShowPlayerRatings())
              ),
              "showRatings"
            ),
            setting(
              trp.showFlairs(),
              radios(form("flairs"), translatedBooleanChoices),
              "showFlairs"
            )
          ),
          categFieldset(PrefCateg.ChessClock, categ)(
            setting(
              trp.tenthsOfSeconds(),
              radios(form("clock.tenths"), translatedClockTenthsChoices),
              "tenthsOfSeconds"
            ),
            setting(
              trp.horizontalGreenProgressBars(),
              radios(form("clock.bar"), booleanChoices),
              "horizontalGreenProgressBars"
            ),
            setting(
              trp.soundWhenTimeGetsCritical(),
              radios(form("clock.sound"), booleanChoices),
              "soundWhenTimeGetsCritical"
            ),
            setting(
              trp.giveMoreTime(),
              radios(form("clock.moretime"), translatedMoretimeChoices),
              "giveMoreTime"
            ),
            setting(
              trp.showClockOnTheLeft(),
              radios(form("clock.swapClock"), booleanChoices),
              "swapClock"
            )
          ),
          categFieldset(PrefCateg.GameBehavior, categ)(
            setting(
              trp.howDoYouMovePieces(),
              radios(form("behavior.moveEvent"), translatedMoveEventChoices),
              "howDoYouMovePieces"
            ),
            setting(
              trp.premovesPlayingDuringOpponentTurn(),
              radios(form("behavior.premove"), booleanChoices),
              "premovesPlayingDuringOpponentTurn"
            ),
            setting(
              trp.takebacksWithOpponentApproval(),
              radios(form("behavior.takeback"), translatedTakebackChoices),
              "takebacksWithOpponentApproval"
            ),
            setting(
              trp.promoteToQueenAutomatically(),
              frag(
                radios(form("behavior.autoQueen"), translatedAutoQueenChoices),
                div(cls := "help text shy", dataIcon := Icon.InfoCircle)(
                  trp.explainPromoteToQueenAutomatically()
                )
              ),
              "promoteToQueenAutomatically"
            ),
            setting(
              trp.claimDrawOnThreefoldRepetitionAutomatically(),
              radios(form("behavior.autoThreefold"), translatedAutoThreefoldChoices),
              "claimDrawOnThreefoldRepetitionAutomatically"
            ),
            setting(
              trp.moveConfirmation(),
              frag(
                bitCheckboxes(form("behavior.submitMove"), submitMoveChoices),
                div(cls := "help text shy", dataIcon := Icon.InfoCircle)(
                  "Multiple choices. ",
                  trp.explainCanThenBeTemporarilyDisabled()
                )
              ),
              "moveConfirmation"
            ),
            setting(
              trp.confirmResignationAndDrawOffers(),
              radios(form("behavior.confirmResign"), confirmResignChoices),
              "confirmResignationAndDrawOffers"
            ),
            setting(
              trp.castleByMovingTheKingTwoSquaresOrOntoTheRook(),
              radios(form("behavior.rookCastle"), translatedRookCastleChoices),
              "castleByMovingTheKingTwoSquaresOrOntoTheRook"
            ),
            setting(
              trp.inputMovesWithTheKeyboard(),
              radios(form("behavior.keyboardMove"), booleanChoices),
              "inputMovesWithTheKeyboard"
            ),
            setting(
              trp.inputMovesWithVoice(),
              radios(form("behavior.voice"), booleanChoices),
              "inputMovesWithVoice"
            ),
            setting(
              trp.snapArrowsToValidMoves(),
              radios(form("behavior.arrowSnap"), booleanChoices),
              "snapArrowsToValidMoves"
            )(cls := "arrow-snap"),
            setting(
              trp.sayGgWpAfterLosingOrDrawing(),
              radios(form("behavior.sayGG"), translatedSayGGChoices),
              "sayGG"
            ),
            setting(
              trp.scrollOnTheBoardToReplayMoves(),
              radios(form("behavior.scrollMoves"), booleanChoices),
              "scrollOnTheBoardToReplayMoves"
            )
          ),
          categFieldset(PrefCateg.Privacy, categ)(
            setting(
              trans.site.letOtherPlayersFollowYou(),
              radios(form("follow"), booleanChoices),
              "letOtherPlayersFollowYou"
            ),
            setting(
              trans.site.letOtherPlayersChallengeYou(),
              radios(form("challenge"), translatedChallengeChoices),
              "letOtherPlayersChallengeYou"
            ),
            setting(
              trans.site.letOtherPlayersMessageYou(),
              radios(form("message"), translatedMessageChoices),
              "letOtherPlayersMessageYou"
            ),
            setting(
              trans.site.letOtherPlayersInviteYouToStudy(),
              radios(form("studyInvite"), translatedStudyInviteChoices),
              "letOtherPlayersInviteYouToStudy"
            ),
            setting(
              trans.site.shareYourInsightsData(),
              radios(form("insightShare"), translatedInsightShareChoices),
              "shareYourInsightsData"
            )
          ),
          p(cls := "saved text none", dataIcon := Icon.Checkmark)(trp.yourPreferencesHaveBeenSaved())
        )
      )

  object notification:

    def apply(form: Form[?])(using Context) =
      AccountPage(
        s"${trans.preferences.notifications.txt()} - ${trp.preferences.txt()}",
        "notification"
      ):
        div(cls := "box box-pad")(
          h1(cls := "box__top")(trans.preferences.notifications()),
          postForm(cls := "autosubmit", action := routes.Pref.notifyFormApply)(
            div(
              table(cls := "allows")(
                thead(
                  tr(
                    th,
                    th(trp.notifyBell(), iconTag(Icon.BellOutline)),
                    th(trp.notifyPush(), iconTag(Icon.PhoneMobile))
                  )
                ),
                tbody(
                  List(
                    a(href := routes.Streamer.index())(trp.notifyStreamStart()) -> "streamStart",
                    trp.notifyForumMention() -> "mention",
                    trp.notifyInvitedStudy() -> "invitedStudy",
                    trp.notifyInboxMsg() -> "privateMessage",
                    trp.notifyChallenge() -> "challenge",
                    trp.notifyTournamentSoon() -> "tournamentSoon",
                    frag("Broadcasts") -> "broadcastRound",
                    trp.notifyGameEvent() -> "gameEvent"
                  ).map(makeRow(form))
                )
              ),
              div(
                id := "correspondence-email-notif"
              )( // id is set to allow direct unsubcribe link in correspondence emails
                bits.setting(
                  trp.correspondenceEmailNotification(),
                  radios(form("correspondenceEmail"), translatedBooleanChoices)
                )
              ),
              bits.setting(
                trp.bellNotificationSound(),
                radios(form("notification.playBellSound"), translatedBooleanIntChoices)
              )
            ),
            p(cls := "saved text none", dataIcon := Icon.Checkmark)(trp.yourPreferencesHaveBeenSaved())
          )
        )

    private def makeRow(form: play.api.data.Form[?])(transFrag: Frag, filterName: String) =
      tr(
        td(transFrag),
        Seq("bell", "push").map: allow =>
          val name = s"$filterName.$allow"
          val checked = form.data(name).contains("true")
          td(
            if !hiddenFields(s"$filterName.$allow") then
              div(cls := "toggle", form3.cmnToggle(name, name, checked))
            else if !checked then div(iconTag(Icon.X))
            else
              div(
                cls := "always-on",
                form3.hidden(name, "true"),
                filterName match
                  case "challenge" => iconTag(Icon.Swords)
                  case "privateMessage" => iconTag(Icon.BellOutline)
                  case _ => emptyFrag
              )
          )
      )

    private val hiddenFields = Set(
      "privateMessage.bell",
      "tournamentSoon.bell",
      "gameEvent.bell",
      "challenge.bell"
    )

  def network(cfRouting: Boolean)(using ctx: Context) =
    AccountPage("Network", "network"):
      div(cls := "box box-pad")(
        standardFlash,
        h1(cls := "box__top")("Network"),
        flashMessage("quiet")(
          "You are currently using ",
          if cfRouting then "Content Delivery Network (CDN) routing."
          else "direct routing.",
          br,
          if cfRouting
          then "This feature is experimental but may improve reliability in some regions."
          else "If you have frequent disconnects, Content Delivery Network (CDN) routing may improve things."
        ),
        br,
        postForm(action := routes.Pref.networkPost):
          button(
            name := "cfRouting",
            value := !cfRouting,
            cls := "button"
          )(if cfRouting then "Use direct routing" else "Use CDN routing")
      )
