package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object faq {

  import trans.faq._

  private val fideHandbook = "https://www.fide.com/FIDE/handbook/LawsOfChess.pdf"

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
        h1(cls := "lichess_title")(frequentlyAskedQuestions()),
        h2("Lichess"),
        question(
          "name",
          whyIsLichessCalledLichess.txt(),
          p(
            lichessCombinationLiveLightLibrePronounced(em(leechess())),
            a(href := "https://www.youtube.com/watch?v=KRpPqcrdE-o")(hearItPronouncedBySpecialist())
          ),
          p(
            whyLiveLightLibre()
          ),
          p(
            whyIsLilaCalledLila(
              a(href := "https://github.com/ornicar/lila")("lila"),
              a(href := "https://www.scala-lang.org/")("Scala")
            )
          )
        ),
        question(
          "contributing",
          howCanIContributeToLichess.txt(),
          p(lichessPoweredByDonationsAndVolunteers()),
          p(
            findMoreAndSeeHowHelp(
            a(href := routes.Plan.index())(beingAPatron()),
            a(href := routes.Main.costs())(breakdownOfOurCosts()),
            a(href := routes.Page.help())(otherWaysToHelp())
            )
          )
        ),
        question(
          "sites_based_on_Lichess",
          areThereWebsitesBasedOnLichess.txt(),
          p(
            yesLichessInspiredOtherOpenSourceWebsites(
            a(href := "/source")(trans.sourceCode()),
            a(href := "/api")("API"),
            a(href := "https://database.lichess.org")(trans.database())
            )
          ),
          ul(
            li(a(href := "https://blitztactics.com/about")("Blitz Tactics")),
            li(a(href := "https://tailuge.github.io/chess-o-tron/html/blunder-bomb.html")("Blunder Bomb")),
            li(a(href := "https://lidraughts.org")("lidraughts.org"))
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
        h2("Gameplay"),
        question(
          "time-controls",
          howBulletBlitzEtcDecided.txt(),
          p(
            basedOnGameDuration(strong(durationFormula()))
          ),
          ul(
            li(inferiorThanXsEqualYtimeControl(30,"UltraBullet")),
            li(inferiorThanXsEqualYtimeControl(180,"Bullet")),
            li(inferiorThanXsEqualYtimeControl(480,"Blitz")),
            li(inferiorThanXsEqualYtimeControl(1500,trans.rapid())),
            li(superiorThanXsEqualYtimeControl(1500,trans.classical()))
          )
        ),
        question(
          "variants",
          whatVariantsCanIplay.txt(),
          p(
            lichessSupportChessAnd(
            a(href := routes.Page.variantHome())(eightVariants())
            )
          )
        ),
        question(
          "acpl",
          whatIsACPL.txt(),
          p(
            acplExplanation()
           )
        ),
        question(
          "timeout",
          insufficientMaterial.txt(),
          p(
            lichessFollowFIDErules(
              a(href := fideHandbook)(linkToFIDErules()))
          )
        ),
        question(
          "en-passant",
          discoveringEnPassant.txt(),
          p(
            explainingEnPassant(
            a(href := "https://en.wikipedia.org/wiki/En_passant")(goodIntroduction()),
            a(href := fideHandbook)(officialRulesPDF()),
            a(href := s"${routes.Learn.index()}#/15")(lichessTraining())
            )
          )
        ),
        question(
          "threefold",
          threefoldRepetition.txt(),
          p(
            threefoldRepetitionExplanation(
            a(href := "https://en.wikipedia.org/wiki/Threefold_repetition")(threefoldRepetitionLowerCase()),
            a(href := fideHandbook)(handBookPDF())
            )
          ),
          h4(notRepeatedMoves()),
          p(
            repeatedPositionsThatMatters(
              em(positions())
            )
          ),
          h4(weRepeatedthreeTimesPosButNoDraw()),
          p(
            threeFoldHasToBeClaimed(
              a(href := routes.Pref.form("game-behavior"))(configure()),
            )
          )
        ),
        h2(accounts()),
        question(
          "titles",
          titlesAvailableOnLichess.txt(),
          p(
            lichessRecognizeAllOTBtitles(
            a(href := "https://github.com/ornicar/lila/wiki/Handling-title-verification-requests")(
              asWellAsManyNMtitles()
            )
          )
          ),
          ul(
            li("Grandmaster (GM)"),
            li("International Master (IM)"),
            li("FIDE Master (FM)"),
            li("Candidate Master (CM)"),
            li("Woman Grandmaster (WGM)"),
            li("Woman International Master (WIM)"),
            li("Woman FIDE Master (WFM)"),
            li("Woman Candidate Master (WCM)")
          ),
          p(
            showYourTitle(
            a(href := routes.Main.verifyTitle())(verificationForm()),
            a(href := "#lm")(lichessMasterLM())
            )
          )
        ),
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
        question(
          "trophies",
          uniqueTrophies.txt(),
          h4("The way of Berserk"),
          p(
            ownerUniqueTrophies(
              a(href := "https://lichess.org/@/hiimgosu")("hiimgosu")
            )
          ),
          p(
            wayOfBerserkExplanation(
              a(href := "https://lichess.org/tournament/cDyjj1nL")(aHourlyBulletTournament())
            )
          ),
          h4("The Golden Zee"),
          p(
            ownerUniqueTrophies(
              a(href := "https://lichess.org/@/ZugAddict")("ZugAddict")
            )
          ),
          p(
            goldenZeeExplanation()
          )
        ),
        h2("Lichess ratings"),
        question(
          "ratings",
          whichRatingSystemUsedByLichess.txt(),
          p(
            ratingSystemUsedByLichess()
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
          "How do ranks and leaderboards work?",
          p("In order to get on the ", a(href := routes.User.list())("rating leaderboards"), ", you must:"),
          ol(
            li("have played at least 30 rated games in a given rating,"),
            li("have played a rated game within the last week for this rating,"),
            li(
              "have a rating deviation lower than ",
              lila.rating.Glicko.standardRankableDeviation,
              " in standard chess, and lower than ",
              lila.rating.Glicko.variantRankableDeviation,
              " in variants,"
            ),
            li("be in the top 10 in this rating.")
          ),
          p(
            "The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards."
          )
        ),
        question(
          "high-ratings",
          "Why are ratings higher compared to other sites and organisations such as FIDE, USCF and the ICC?",
          p(
            "It is best not to think of ratings as absolute numbers, or compare them against other organisations. Different organisations have different levels of players, different rating systems (Elo, Glicko, Glicko-2, or a modified version of the aforementioned). These factors can drastically affect the absolute numbers (ratings)."
          ),
          p(
            """It's best to think of ratings as "relative" figures (as opposed to "absolute" figures): Within a pool of players, their relative differences in ratings will help you estimate who will win/draw/lose, and how often. Saying "I have X rating" means nothing unless there are other players to compare that rating to."""
          )
        ),
        question(
          "hide-ratings",
          "How to hide ratings while playing?",
          p(
            "Enable Zen-mode in the ",
            a(href := routes.Pref.form("game-display"))("display preferences"),
            " or by pressing ",
            em("z"),
            " during a game."
          )
        ),
        question(
          "disconnection-loss",
          "I lost a game due to lag/disconnection. Can I get my rating points back?",
          p(
            "Unfortunately, we cannot give back rating points for games lost due to lag or disconnection, regardless of whether the problem was at your end or our end. The latter is very rare though. Also note that when Lichess restarts and you lose on time because of that, we abort the game to prevent an unfair loss."
          )
        ),
        h2("How to..."),
        question(
          "browser-notifications",
          "Enable or disable notification popups?",
          p(img(src := assetUrl("images/connection-info.png"), alt := "View site information popup")),
          p(
            "Lichess can optionally send popup notifications, for example when it is your turn or you received a private message."
          ),
          p("Click the lock icon next to the lichess.org address in the URL bar of your browser."),
          p("Then select whether to allow or block notifications from Lichess.")
        )
      )
    }
}
