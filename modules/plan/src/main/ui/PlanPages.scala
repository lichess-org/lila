package lila.plan
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PlanPages(helpers: Helpers)(fishnetPerDay: Int):
  import helpers.{ *, given }
  import trans.patron as trp

  def features(using Context) =
    def header(name: Frag)(using Translate) = thead(
      st.tr(th(name), th(trp.freeAccount()), th(trp.lichessPatron()))
    )
    val unlimited =
      span(dataIcon := Icon.Checkmark, cls := "is is-green text unlimited")(trans.site.unlimited())
    val custom = span(dataIcon := Icon.Checkmark, cls := "is is-green text check")
    val check = custom(trans.site.yes())
    def all(content: Frag) = frag(td(content), td(content))
    def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))
    val title = "Lichess features"
    Page(title)
      .css("bits.feature")
      .graph(
        title = title,
        url = s"$netBaseUrl${routes.Plan.features.url}",
        description = "All of Lichess features are free for all and forever. We do it for the chess!"
      ):
        main(cls := "box box-pad features")(
          table(
            header(h1(dataIcon := Icon.ScreenDesktop)(trans.site.website())),
            tbody(
              tr(check)(
                strong(trans.features.zeroAdsAndNoTracking())
              ),
              tr(unlimited)(
                a(href := routes.Tournament.home)(trans.arena.arenaTournaments())
              ),
              tr(unlimited)(
                a(href := routes.Swiss.home)(trans.swiss.swissTournaments())
              ),
              tr(unlimited)(
                a(href := routes.Simul.home)(trans.site.simultaneousExhibitions())
              ),
              tr(unlimited)(
                trans.features.correspondenceWithConditionalPremoves()
              ),
              tr(check)(
                trans.features.standardChessAndX(a(href := routes.Cms.variantHome)(trans.faq.eightVariants()))
              ),
              tr(custom(trans.features.gamesPerDay.pluralSame(fishnetPerDay)))(
                trans.features.deepXServerAnalysis(lila.ui.bits.engineFullName)
              ),
              tr(unlimited)(
                trans.features.boardEditorAndAnalysisBoardWithEngine("Stockfish 16+ NNUE")
              ),
              tr(unlimited)(
                a(href := "https://lichess.org/blog/WN-gLzAAAKlI89Xn/thousands-of-stockfish-analysers")(
                  trans.features.cloudEngineAnalysis()
                )
              ),
              tr(unlimited)(
                a(href := "https://lichess.org/blog/WFvLpiQAACMA8e9D/learn-from-your-mistakes")(
                  trans.site.learnFromYourMistakes()
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
                a(href := s"${routes.UserAnalysis.index}#explorer")(
                  trans.features.globalOpeningExplorerInNbGames(6_000_000_000L.localize)
                )
              ),
              tr(check)(
                trans.features.personalOpeningExplorerX(
                  a(href := s"${routes.UserAnalysis.index}#explorer/me")(
                    trans.features.personalOpeningExplorer()
                  ),
                  a(href := s"${routes.UserAnalysis.index}#explorer/DrNykterstein")(trans.site.otherPlayers())
                )
              ),
              tr(unlimited)(
                a(href := s"${routes.UserAnalysis.parseArg("QN4n1/6r1/3k4/8/b2K4/8/8/8_b_-_-")}#explorer")(
                  trans.features.endgameTablebase()
                )
              ),
              tr(check)(
                trans.features.downloadOrUploadAnyGameAsPgn()
              ),
              tr(check)(
                trans.features.tvForumBlogTeamsMessagingFriendsChallenges()
              ),
              tr(check)(
                trans.site.availableInNbLanguages(a(href := "https://crowdin.com/project/lichess")("140+"))
              ),
              tr(check)(
                trans.features.lightOrDarkThemeCustomBoardsPiecesAndBackground()
              ),
              tr(check)(
                strong(trans.features.allFeaturesToCome())
              )
            ),
            header(h1(dataIcon := Icon.PhoneMobile)(trans.site.mobile())),
            tbody(
              tr(check)(
                strong(trans.features.zeroAdsAndNoTracking())
              ),
              tr(unlimited)(
                trans.features.ultraBulletBulletBlitzRapidClassicalAndCorrespondenceChess()
              ),
              tr(unlimited)(
                a(href := routes.Tournament.home)(trans.arena.arenaTournaments())
              ),
              tr(check)(
                trans.features.boardEditorAndAnalysisBoardWithEngine("Stockfish 14+")
              ),
              tr(unlimited)(
                a(href := routes.Puzzle.home)(trans.features.tacticalPuzzlesFromUserGames())
              ),
              tr(check)(
                trans.site.availableInNbLanguages(a(href := "https://crowdin.com/project/lichess")("100+"))
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
            )
          ),
          p(cls := "explanation")(
            strong(trans.features.everybodyGetsAllFeaturesForFree()),
            br,
            trans.features.weBelieveEveryChessPlayerDeservesTheBest(),
            br,
            br,
            strong(trans.features.allFeaturesAreFreeForEverybody()),
            br,
            trans.features.ifYouLoveLichess(),
            a(cls := "button", href := routes.Plan.index())(trans.features.supportUsWithAPatronAccount())
          )
        )

  def thanks(
      patron: Option[Patron],
      stripeCustomer: Option[StripeCustomer],
      gift: Option[Patron]
  )(using ctx: Context) =
    Page(trans.patron.thankYou.txt())
      .css("bits.page"):
        main(cls := "page-small page box box-pad")(
          boxTop(h1(cls := "text", dataIcon := patronIconChar)(trp.thankYou())),
          p(trp.tyvm()),
          p(trp.transactionCompleted()),
          (gift, patron) match
            case (Some(gift), _) =>
              p(
                userIdLink(gift.userId.some),
                " ",
                if gift.isLifetime then "is now a lifetime Lichess Patron"
                else "is now a Lichess Patron for one month",
                ", thanks to you!"
              )
            case (_, Some(pat)) =>
              if pat.payPal.exists(_.renew) ||
                pat.payPalCheckout.exists(_.renew) ||
                stripeCustomer.exists(_.renew)
              then
                ctx.me.map { me =>
                  p(
                    trp.permanentPatron(),
                    br,
                    a(href := routes.User.show(me.username))(trp.checkOutProfile())
                  )
                }
              else
                frag(
                  if pat.isLifetime then
                    p(
                      trp.nowLifetime(),
                      br,
                      ctx.me.map { me =>
                        a(href := routes.User.show(me.username))(trp.checkOutProfile())
                      }
                    )
                  else
                    frag(
                      p(
                        trp.nowOneMonth(),
                        br,
                        ctx.me.map { me =>
                          a(href := routes.User.show(me.username))(trp.checkOutProfile())
                        }
                      ),
                      p(trp.downgradeNextMonth())
                    )
                )
            case _ => emptyFrag
          ,
          br,
          br,
          br,
          br,
          br,
          br,
          a(href := s"${routes.Plan.list}?dest=gift")(trp.makeAdditionalDonation())
        )
