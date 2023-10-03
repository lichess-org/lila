package views
package html.site

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object faq:

  import trans.faq.*

  val fideHandbookUrl = "https://handbook.fide.com/chapter/E012023"

  private def question(id: String, title: String, answer: Frag*) =
    div(
      st.id := id,
      cls   := "question"
    )(
      h3(a(href := s"#$id")(title)),
      div(cls := "answer")(answer)
    )

  def apply()(using PageContext) =
    page.layout(
      title = "Frequently Asked Questions",
      active = "faq",
      moreCss = cssTag("faq")
    ) {
      div(cls := "faq small-page box box-pad")(
        h1(cls := "box__top")(frequentlyAskedQuestions()),
        h2("Lichess"),
        question(
          "name",
          whyIsLichessCalledLichess.txt(),
          p(
            lichessCombinationLiveLightLibrePronounced(em(leechess())),
            " ",
            a(href := "https://www.youtube.com/watch?v=KRpPqcrdE-o")(hearItPronouncedBySpecialist())
          ),
          p(
            whyLiveLightLibre()
          ),
          p(
            whyIsLilaCalledLila(
              a(href := "https://github.com/lichess-org/lila")("lila"),
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
              a(href := routes.Plan.index)(beingAPatron()),
              a(href := routes.Main.costs)(breakdownOfOurCosts()),
              a(href := routes.ContentPage.help)(otherWaysToHelp())
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
            li(a(href := "https://lidraughts.org")("lidraughts.org")),
            li(a(href := "https://playstrategy.org")("playstrategy.org")),
            li(a(href := "https://lishogi.org")("lishogi.org"))
          )
        ),
        question(
          "keyboard-shortcuts",
          keyboardShortcuts.txt(),
          p(
            keyboardShortcutsExplanation()
          )
        ),
        h2(fairPlay()),
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
          ),
          p(
            pleaseReadFairPlayPage(a(href := routes.ContentPage.loneBookmark("fair-play"))(fairPlayPage()))
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
            li(inferiorThanXsEqualYtimeControl(29, "UltraBullet")),
            li(inferiorThanXsEqualYtimeControl(179, "Bullet")),
            li(inferiorThanXsEqualYtimeControl(479, "Blitz")),
            li(inferiorThanXsEqualYtimeControl(1499, trans.rapid())),
            li(superiorThanXsEqualYtimeControl(1500, trans.classical()))
          )
        ),
        question(
          "variants",
          whatVariantsCanIplay.txt(),
          p(
            lichessSupportChessAnd(
              a(href := routes.ContentPage.variantHome)(eightVariants())
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
            lichessFollowFIDErules(a(href := fideHandbookUrl)(fideHandbookX("§6.9")))
          )
        ),
        question(
          "en-passant",
          discoveringEnPassant.txt(),
          p(
            explainingEnPassant(
              a(href := "https://en.wikipedia.org/wiki/En_passant")(goodIntroduction()),
              a(href := fideHandbookUrl)(fideHandbook()),
              a(href := s"${routes.Learn.index}#/15")(lichessTraining())
            )
          ),
          p(
            watchIMRosenCheckmate(
              a(href := "https://www.reddit.com/r/AnarchyChess/comments/p9wuic/eric_rosen_ascending/")(
                "en passant"
              )
            )
          )
        ),
        question(
          "threefold",
          threefoldRepetition.txt(),
          p(
            threefoldRepetitionExplanation(
              a(href := "https://en.wikipedia.org/wiki/Threefold_repetition")(threefoldRepetitionLowerCase()),
              a(href := fideHandbookUrl)(fideHandbook())
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
              a(href := routes.Pref.form("game-behavior"))(configure())
            )
          )
        ),
        h2(accounts()),
        question(
          "titles",
          titlesAvailableOnLichess.txt(),
          p(
            lichessRecognizeAllOTBtitles(
              a(href := "https://github.com/lichess-org/lila/wiki/Handling-title-verification-requests")(
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
              a(href := routes.Main.verifyTitle)(verificationForm()),
              a(href := "#lm")("Lichess master (LM)")
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
              a(href := "https://github.com/lichess-org/lila/wiki/Username-policy")(guidelines())
            )
          )
        ),
        question(
          "change-username",
          canIChangeMyUsername.txt(),
          p(usernamesCannotBeChanged.txt())
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
        h2(lichessRatings()),
        question(
          "ratings",
          whichRatingSystemUsedByLichess.txt(),
          p(
            ratingSystemUsedByLichess()
          ),
          p(
            a(href := routes.ContentPage.loneBookmark("rating-systems"))("More about rating systems")
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
          ),
          p(
            a(href := routes.ContentPage.loneBookmark("rating-systems"))("More about rating systems")
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
            lichessCanOptionnalySendPopUps()
          )
        ),
        question(
          "make-a-bot",
          "Make a Lichess bot?",
          p(
            "To learn how to create a ",
            a(href := "https://lichess.org/blog/WvDNticAAMu_mHKP/welcome-lichess-bots")("Lichess bot"),
            ", please read ",
            a(href := "https://lichess.org/@/thibault/blog/how-to-create-a-lichess-bot/FuKyvDuB")(
              "this blog post"
            ),
            "."
          )
        ),
        question(
          "autoplay",
          "Enable autoplay for sounds?",
          p("""
            Most browsers can prevent sound from playing on a freshly loaded page to protect users. 
            Imagine if every website could immediately bombard you with audio ads.
          """),
          p("""
            The red mute icon appears when your browser prevents lichess.org from playing a sound.
            Usually this restriction is lifted once you click something. On some mobile browsers,
            touch dragging a piece does not count as a click. In that case you must tap the board to
            allow sound at the start of each game.
          """),
          p("""
            We show the red icon to alert you when this happens. Often you can explicitly allow
            lichess.org to play sounds. Here are instructions for doing so on recent versions of
            some popular browsers.
          """),
          h3("Mozilla Firefox (desktop):"),
          ul(
            li("Go to lichess.org"),
            li("Press ctrl-i on linux/windows or cmd-i on macos"),
            li("Click the Permissions tab"),
            li("Allow Audio and Video on lichess.org")
          ),
          h3("Google Chrome (desktop):"),
          ul(
            li("Go to lichess.org"),
            li("Click the lock icon in the address bar"),
            li("Click Site Settings"),
            li("Allow Sound")
          ),
          h3("Safari (desktop):"),
          ul(
            li("Go to lichess.org"),
            li("Click Safari in the menu bar"),
            li("Click Settings for This Website"),
            li("Allow Auto-Play")
          ),
          h3("Microsoft Edge (desktop):"),
          ul(
            li("Click the three dots in the top right corner"),
            li("Click Settings"),
            li("Click Cookies and Site Permissions"),
            li("Scroll down and click Media autoplay"),
            li("Add lichess.org to Allow")
          )
        )
      )
    }
