package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object faq {

  import trans.faq._

  private def question(id: String, title: String, answer: Frag*) =
    div(
      st.id := id,
      cls   := "question"
    )(
      h3(a(href := s"#$id")(title)),
      div(cls := "answer")(answer)
    )

  def apply()(implicit ctx: Context) =
    help.layout(
      title = trans.faq.frequentlyAskedQuestions.txt(),
      active = "faq",
      moreCss = cssTag("faq")
    ) {
      main(cls := "faq small-page box box-pad")(
        h1(cls := "lishogi_title")(frequentlyAskedQuestions()),
        h2("Lishogi"),
        question(
          "contributing",
          howCanIContributeToLishogi.txt(),
          p(lishogiPoweredByDonationsAndVolunteers()),
          p(
            a(href := routes.Plan.index)(beingAPatron())
          ),
          p(
            a(href := routes.Page.help)(otherWaysToContribute())
          )
        ),
        h2(fairPlay()),
        question(
          "marks",
          whyFlaggedRatingManipulationOrCheater.txt(),
          p(
            cheatDetectionMethods(contactEmail)
          )
        ),
        question(
          "rating-refund",
          whenAmIEligibleRatinRefund.txt(),
          p(
            ratingRefundExplanation()
          )
        ),
        question(
          "leaving",
          preventLeavingGameWithoutResigning.txt(),
          p(
            leavingGameWithoutResigningExplanation()
          )
        ),
        question(
          "mod-application",
          howCanIBecomeModerator.txt(),
          p(
            youCannotApply()
          )
        ),
        question(
          "correspondence",
          isCorrespondenceDifferent.txt(),
          p(
            youCanUseOpeningBookNoEngine()
          )
        ),
        h2(gameplay()),
        question(
          "time-controls",
          howBulletBlitzEtcDecided.txt(),
          p(
            basedOnGameDuration(strong(formulaOfDuration()))
          ),
          ul(
            li(inferiorThanXsEqualYtimeControl(60, trans.ultrabullet())),
            li(inferiorThanXsEqualYtimeControl(300, trans.bullet())),
            li(inferiorThanXsEqualYtimeControl(600, trans.blitz())),
            li(inferiorThanXsEqualYtimeControl(1500, trans.rapid())),
            li(superiorThanXsEqualYtimeControl(1500, trans.classical()))
          )
        ),
        h2(accounts()),
        question(
          "lm",
          canIbecomeLM.txt(),
          p(strong(noUpperCaseDot())),
          p(lMtitleComesToYouDoNotRequestIt())
        ),
        question(
          "usernames",
          whatUsernameCanIchoose.txt(),
          p(
            usernamesNotOffensive(
              a(href := "https://github.com/lichess-org/lila/wiki/Username-policy")(guidelines())
            )
          )
        ),
        h2(lishogiRatings()),
        question(
          "ratings",
          whichRatingSystemUsedByLishogi.txt(),
          p(
            ratingSystemUsedByLishogi()
          )
        ),
        question(
          "provisional",
          whatIsProvisionalRating.txt(),
          p(provisionalRatingExplanation()),
          ul(
            li(
              notPlayedEnoughRatedGamesAgainstX(
                em(similarOpponents())
              )
            ),
            li(
              notPlayedRecently()
            )
          ),
          p(
            ratingDeviationMorethanOneHundredTen()
          )
        ),
        question(
          "leaderboards",
          howDoLeaderoardsWork.txt(),
          p(
            inOrderToAppearsYouMust(
              a(href := routes.User.list)(ratingLeaderboards())
            )
          ),
          ol(
            li(havePlayedMoreThanThirtyGamesInThatRating()),
            li(havePlayedARatedGameAtLeastOneWeekAgo()),
            li(
              ratingDeviationLowerThanXinShogiYinVariants(
                lila.rating.Glicko.standardRankableDeviation,
                lila.rating.Glicko.variantRankableDeviation
              )
            ),
            li(beInTopTen())
          ),
          p(
            secondRequirementToStopOldPlayersTrustingLeaderboards()
          )
        ),
        question(
          "high-ratings",
          whyAreRatingHigher.txt(),
          p(
            whyAreRatingHigherExplanation()
          )
        ),
        question(
          "hide-ratings",
          howToHideRatingWhilePlaying.txt(),
          p(
            enableZenMode(
              a(href := routes.Pref.form("game-display"))(displayPreferences()),
              em("z")
            )
          )
        ),
        question(
          "disconnection-loss",
          connexionLostCanIGetMyRatingBack.txt(),
          p(
            weCannotDoThatEvenIfItIsServerSideButThatsRare()
          )
        ),
        h2(howToThreeDots()),
        question(
          "browser-notifications",
          enableDisableNotificationPopUps.txt(),
          p(img(src := assetUrl("images/connection-info.png"), alt := viewSiteInformationPopUp.txt())),
          p(
            lishogiCanOptionnalySendPopUps()
          )
        )
      )
    }
}
