package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object faq {

  import trans.faq._

  private val fideHandbook = "http://www.shogi.net/fesa/pdf/FESA%20rules.pdf"

  private def question(id: String, title: String, answer: Frag*) =
    div(
      st.id := id,
      cls := "question"
    )(
      h3(a(href := s"#$id")(title)),
      div(cls := "answer")(answer)
    )

  def apply()(implicit ctx: Context) =
    help.layout(
      title = "Frequently Asked Questions",
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
            a(href := routes.Page.patron())(beingAPatron()), // patron
          )
        ),
        // question(
        //   "sites_based_on_Lichess",
        //   areThereWebsitesBasedOnLishogi.txt(),
        //   p(
        //     yesLishogiInspiredOtherOpenSourceWebsites(
        //       a(href := "/source")(trans.sourceCode()),
        //       a(href := "/api")("API"),
        //       a(href := "https://database.lishogi.org")(trans.database())
        //     )
        //   ),
        //   ul(
        //     li(a(href := "https://blitztactics.com/about")("Blitz Tactics")),
        //     li(a(href := "https://tailuge.github.io/chess-o-tron/html/blunder-bomb.html")("Blunder Bomb")),
        //     li(a(href := "https://lidraughts.org")("lidraughts.org"))
        //   )
        // ),
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
            basedOnGameDuration(strong(durationFormula()))
          ),
          ul(
            li(inferiorThanXsEqualYtimeControl(30, "UltraBullet")),
            li(inferiorThanXsEqualYtimeControl(180, "Bullet")),
            li(inferiorThanXsEqualYtimeControl(480, "Blitz")),
            li(inferiorThanXsEqualYtimeControl(1500, trans.rapid())),
            li(superiorThanXsEqualYtimeControl(1500, trans.classical()))
          )
        ),
        //question( // todo variant
        //  "variants",
        //  whatVariantsCanIplay.txt(),
        //  p(
        //    lishogiSupportChessAnd(
        //      a(href := routes.Page.variantHome())(eightVariants())
        //    )
        //  )
        //),
        // question(
        //   "timeout",
        //   insufficientMaterial.txt(),
        //   p(
        //     lishogiFollowFIDErules(a(href := fideHandbook)(linkToFIDErules()))
        //   )
        // ),
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
              a(href := "https://github.com/ornicar/lila/wiki/Username-policy")(guidelines())
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
              a(href := routes.User.list())(ratingLeaderboards())
            )
          ),
          ol(
            li(havePlayedMoreThanThirtyGamesInThatRating()),
            li(havePlayedARatedGameAtLeastOneWeekAgo()),
            li(
              ratingDeviationLowerThanXinChessYinVariants(
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
