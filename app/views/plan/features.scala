package views
package html.plan

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object features {

  def apply()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.features.txt(),
      moreCss = cssTag("feature"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.features.txt(),
          url = s"$netBaseUrl${routes.Plan.features.url}",
          description = trans.everybodyGetsAllFeaturesForFree.txt()
        )
        .some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "box box-pad features")(
        table(
          header(h1(dataIcon := "")("Website")),
          tbody(
            tr(check)(
              a(href := routes.Tournament.home)(trans.tournaments())
            ),
            tr(check)(
              a(href := routes.Simul.home)(trans.simultaneousExhibitions())
            ),
            tr(check)(
              trans.correspondenceShogi()
            ),
            tr(check)(
              a(href := routes.Page.variantHome)(trans.variants())
            ),
            tr(check)(
              s"${trans.localAnalysis.txt()} - YaneuraOu & Fairy-Stockfish"
            ),
            tr(check)(
              s"${trans.cloudAnalysis.txt()} - YaneuraOu & Fairy-Stockfish"
            ),
            tr(check)(
              a(href := "https://lishogi.org/study")(
                trans.studyMenu()
              )
            ),
            tr(check)(
              a(href := "https://lishogi.org/blog/post/ZBxnNBAAACIA599h")(
                trans.postGameStudies()
              )
            ),
            tr(check)(
              a(href := routes.Learn.index)(trans.shogiBasics())
            ),
            tr(check)(
              a(href := routes.Puzzle.home)(trans.puzzles())
            ),
            tr(check)(
              a(href := routes.Importer.importGame)(
                trans.importKif(),
                " & ",
                trans.importCsa()
              )
            ),
            tr(check)(
              a(href := routes.Search.index(1))(trans.search.advancedSearch()),
              " - ",
              trans.search.searchInXGames.pluralSameTxt(2_000_000)
            ),
            tr(check)(
              a(href := routes.ForumCateg.index)(trans.forum())
            ),
            tr(check)(
              a(href := routes.Team.all())(trans.team.teams())
            ),
            tr(check)(
              trans.availableInNbLanguages.plural(38, a(href := "https://crowdin.com/project/lishogi")("38"))
            ),
            tr(check)(
              strong(trans.patron.noAdsNoSubs())
            )
          ),
          // header(h1(dataIcon := "")("Mobile")),
          // tbody(
          //  tr(check)(
          //    "Online and offline games, with 8 variants"
          //  ),
          //  tr(check)(
          //    "Bullet, Blitz, Rapid, Classical and Correspondence chess"
          //  ),
          //  tr(check)(
          //    a(href := routes.Tournament.home)("Arena tournaments")
          //  ),
          //  tr(check)(
          //    s"Board editor and analysis board with $engineName"
          //  ),
          //  tr(check)(
          //    a(href := routes.Puzzle.home)("Tactics puzzles")
          //  ),
          //  tr(check)(
          //    "Available in many languages"
          //  ),
          //  tr(check)(
          //    "Light and dark theme, custom boards and pieces"
          //  ),
          //  tr(check)(
          //    "iPhone & Android phones and tablets, landscape support"
          //  ),
          //  tr(check)(
          //    strong("Zero ads, no tracking")
          //  ),
          //  tr(check)(
          //    strong("All features to come, forever")
          //  )
          // ),
          tbody(cls := "support")(
            st.tr(cls := "price")(
              th,
              td(cls := "green")("$0"),
              td(a(href := routes.Plan.index, cls := "green button")(trans.patron.donate()))
            )
          )
        ),
        p(cls := "explanation")(
          strong(trans.everybodyGetsAllFeaturesForFree()),
          br,
          trans.builtForTheLoveOfShogiNotMoney(),
          br,
          br,
          strong(trans.patron.freeShogi()),
          br,
          if (ctx.isAuth) trans.patron.weRelyOnSupport()
          else trans.patron.donationSupport(),
          a(cls := "button", href := routes.Plan.index)(trans.directlySupportLishogi())
        )
      )
    }

  private def header(name: Frag)(implicit lang: Lang) =
    thead(
      st.tr(
        th(name),
        th(trans.patron.freeAccount()),
        th(
          span(dataIcon := patronIconChar, cls := "is text header")(trans.patron.lishogiPatron())
        )
      )
    )

  private def check(implicit lang: Lang) =
    span(dataIcon := "E", cls := "is is-green text check")(trans.yes())

  private def all(content: Frag) = frag(td(content), td(content))

  private def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))

}
