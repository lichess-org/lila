package views
package html.plan

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object features:

  val engineFullName = "Stockfish 16 NNUE"

  def apply()(using PageContext) =
    views.html.base.layout(
      title = trans.features.features.txt(),
      moreCss = cssTag("feature"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.features.features.txt(),
          url = s"$netBaseUrl${routes.Plan.features.url}",
          description = trans.everybodyGetsAllFeaturesForFree.txt()
        )
        .some
    ) {
      main(cls := "box box-pad features")(
        table(
          header(h1(dataIcon := licon.ScreenDesktop)(trans.website())),
          tbody(
            tr(check)(
              strong(trans.features.zeroAdsAndNoTracking())
            ),
            tr(unlimited)(
              a(href := routes.Tournament.home)(trans.features.playAndCreateTournaments()))
            ),
            tr(unlimited)(
              a(href := routes.Tournament.home)(trans.features.playAndCreateSimul()))
            ),
            tr(unlimited)(
              trans.features.correspondenceWithConditionalPremoves()
            ),
            tr(check)(
              trans.features.standardChessAndX(
              a(href := routes.ContentPage.variantHome)(trans.faq.eightVariants()))
            ),
            tr(custom(s"${lila.fishnet.FishnetLimiter.maxPerDay} per day"))(
              trans.features.deepXServerAnalysis(engineFullName)
            ),
            tr(unlimited)(
              trans.features.boardEditorAndAnalysisBoardWithEngine("Stockfish 16, Fairy-Stockfish 14")
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/WN-gLzAAAKlI89Xn/thousands-of-stockfish-analysers")(
                trans.features.cloudEngineAnalysis()
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/WFvLpiQAACMA8e9D/learn-from-your-mistakes")(
                trans.learnFromYourMistakes()
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")(
                trans.features.studies()
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/VmZbaigAABACtXQC/chess-insights")(
                trans.features.chessInsights()
              )
            ),
            tr(check)(
              a(href := routes.Learn.index)(trans.features.allChessBasicsLessons())
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.home)(trans.features.tacticalPuzzlesFromUserGames())
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.streak)("Puzzle Streak"),
              ", ",
              a(href := routes.Storm.home)("Puzzle Storm"),
              ", ",
              a(href := routes.Racer.home)("Puzzle Racer")
            ),
            tr(check)(
              a(href := s"${routes.UserAnalysis.index}#explorer")(trans.features.globalOpeningExplorerInNbGames("4300000"))
            ),
            tr(check)(
              trans.features.personalOpeningExplorerX(
              a(href := s"${routes.UserAnalysis.index}#explorer/me")(trans.features.personalOpeningExplorer()),
              a(href := s"${routes.UserAnalysis.index}#explorer/DrNykterstein")(trans.otherPlayers()))
            ),
            tr(unlimited)(
              a(href := s"${routes.UserAnalysis.parseArg("QN4n1/6r1/3k4/8/b2K4/8/8/8_b_-_-")}#explorer")(
                trans.features.endgameTablebase())
            ),
            tr(check)(
              trans.features.downloadOrUploadAnyGameAsPgn()
            ),
            tr(unlimited)(
              trans.features.xThroughLichessBillionGames(
              a(href := routes.Search.index(1))(trans.search.advancedSearch()))
            ),
            tr(unlimited)(
              a(href := routes.Video.index)(trans.videoLibrary())
            ),
            tr(check)(
              trans.features.tvForumBlogTeamsMessagingFriendsChallenges()
            ),
            tr(check)(
              trans.availableInNbLanguages(a(href := "https://crowdin.com/project/lichess")("140+"))
            ),
            tr(check)(
              trans.features.lightOrDarkThemeCustomBoardsPiecesAndBackground()
            ),
            tr(check)(
              strong(trans.features.allFeaturesToCome())
            )
          ),
          header(h1(dataIcon := licon.PhoneMobile)(trans.mobile())),
          tbody(
            tr(check)(
              strong(trans.features.zeroAdsAndNoTracking())
            ),
            tr(unlimited)(
              trans.onlineAndOfflinePlay()
            ),
            tr(unlimited)(
              trans.features.ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess()
            ),
            tr(unlimited)(
              a(href := routes.Tournament.home)(trans.arena.arenaTournaments())
            ),
            tr(check)(
              trans.features.boardEditorAndAnalysisBoardWithEngine("14+")
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.home)(trans.features.tacticalPuzzlesFromUserGames())
            ),
            tr(check)(
              trans.availableInNbLanguages(a(href := "https://crowdin.com/project/lichess")("100+"))
            ),
            tr(check)(
              trans.features.lightOrDarkThemeCustomBoardsPiecesAndBackground()
            ),
            tr(check)(
              trans.features.landscapeSupportOnApp()
            ),
            tr(check)(
              strong(trans.features.allFeaturesToCome())
            )
          ),
          header(h1(trans.supportLichess())),
          tbody(cls := "support")(
            st.tr(
              th(trans.features.contributeToLichessAndGetIcon()),
              td("-"),
              td(span(dataIcon := patronIconChar, cls := "is is-green text check")(trans.yes()))
            ),
            st.tr(cls := "price")(
              th,
              td(cls := "green")("$0"),
              td(a(href := routes.Plan.index, cls := "green button")("$5/month"))
            )
          )
        ),
        p(cls := "explanation")(
          strong(trans.features.everybodyGetsAllFeaturesForFree()),
          br,
          trans.builtForTheLoveOfChessNotMoney(),
          br,
          trans.features.weBelieveEveryChessPlayerDeservesTheBest(),
          br,
          br,
          strong(trans.features.allFeaturesAreFreeForEverybody()),
          br,
          trans.features.ifYouLoveLichess(),
          a(cls := "button", href := routes.Plan.index)(trans.features.supportUsWithAPatronAccount())
        )
      )
    }

  private def header(name: Frag)(using Lang) =
    thead(
      st.tr(th(name), th(trans.patron.freeAccount()), th(trans.patron.lichessPatron()))
    )

  private def unlimited(implicit lang: Lang) = span(dataIcon := licon.Checkmark, cls := "is is-green text unlimited")(trans.unlimited())

  private def check(implicit lang: Lang) = span(dataIcon := licon.Checkmark, cls := "is is-green text check")(trans.yes())

  private def custom(str: String) = span(dataIcon := licon.Checkmark, cls := "is is-green text check")(str)

  private def all(content: Frag) = frag(td(content), td(content))

  private def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))
