package views.html.base

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object topnav {

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  private def canSeeClasMenu(implicit ctx: Context) =
    ctx.hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))

  def apply()(implicit ctx: Context) =
    st.nav(id := "topnav", cls := "hover")(
      st.section(
        linkTitle(
          "/",
          frag(
            span(cls := "play")(trans.play()),
            span(cls := "home")("lichess.org")
          )
        ),
        div(role := "group")(
          if (ctx.noBot) a(href := "/?any#hook")(trans.createAGame())
          else a(href := "/?any#friend")(trans.playWithAFriend()),
          ctx.noBot option frag(
            a(href := routes.Tournament.home)(trans.arena.arenaTournaments()),
            a(href := routes.Swiss.home)(trans.swiss.swissTournaments()),
            a(href := routes.Simul.home)(trans.simultaneousExhibitions()),
            ctx.pref.hasDgt option a(href := routes.DgtCtrl.index)("DGT board")
          )
        )
      ),
      ctx.noBot option st.section(
        linkTitle(routes.Puzzle.home.path, trans.puzzles()),
        div(role := "group")(
          a(href := routes.Puzzle.home)(trans.puzzles()),
          a(href := routes.Puzzle.dashboard(30, "home"))(trans.puzzle.puzzleDashboard()),
          a(href := routes.Puzzle.streak)("Puzzle Streak"),
          a(href := routes.Storm.home)("Puzzle Storm"),
          a(href := routes.Racer.home)("Puzzle Racer")
        )
      ),
      st.section(
        linkTitle(routes.Practice.index.path, trans.learnMenu()),
        div(role := "group")(
          ctx.noBot option frag(
            a(href := routes.Learn.index)(trans.chessBasics()),
            a(href := routes.Practice.index)(trans.practice()),
            a(href := routes.Coordinate.home)(trans.coordinates.coordinates())
          ),
          a(href := routes.Study.allDefault(1))(trans.studyMenu()),
          ctx.noKid option a(href := routes.Coach.all(1))(trans.coaches()),
          canSeeClasMenu option a(href := routes.Clas.index)(trans.clas.lichessClasses())
        )
      ),
      st.section(
        linkTitle(routes.Tv.index.path, trans.watch()),
        div(role := "group")(
          a(href := routes.Tv.index)("Lichess TV"),
          a(href := routes.Tv.games)(trans.currentGames()),
          ctx.noKid option a(href := routes.Streamer.index())(trans.streamersMenu()),
          a(href := routes.RelayTour.index())(trans.broadcast.broadcasts()),
          ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary())
        )
      ),
      st.section(
        linkTitle(routes.User.list.path, trans.community()),
        div(role := "group")(
          a(href := routes.User.list)(trans.players()),
          a(href := routes.Team.home())(trans.team.teams()),
          ctx.noKid option a(href := routes.ForumCateg.index)(trans.forum()),
          ctx.me.exists(!_.kid) option a(href := routes.Plan.index)(trans.patron.donate())
        )
      ),
      st.section(
        linkTitle(routes.UserAnalysis.index.path, trans.tools()),
        div(role := "group")(
          a(href := routes.UserAnalysis.index)(trans.analysis()),
          a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer()),
          a(href := routes.Editor.index)(trans.boardEditor()),
          a(href := routes.Importer.importGame)(trans.importGame()),
          a(href := routes.Search.index())(trans.search.advancedSearch())
        )
      )
    )
}
