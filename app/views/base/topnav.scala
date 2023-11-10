package views.html.base

import controllers.clas.routes.{ Clas as clasRoutes }
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object topnav:

  private def linkTitle(url: String, name: Frag)(using ctx: Context) =
    if ctx.blind then h3(name) else a(href := url)(name)

  private def canSeeClasMenu(using ctx: PageContext) =
    ctx.hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))

  def apply()(using ctx: PageContext) =
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
          if ctx.noBot then a(href := s"${langHref("/")}?any#hook")(trans.createAGame())
          else a(href := "/?any#friend")(trans.playWithAFriend()),
          ctx.noBot option frag(
            a(href := langHref(routes.Tournament.home))(trans.arena.arenaTournaments()),
            a(href := langHref(routes.Swiss.home))(trans.swiss.swissTournaments()),
            a(href := langHref(routes.Simul.home))(trans.simultaneousExhibitions()),
            ctx.pref.hasDgt option a(href := routes.DgtCtrl.index)(trans.dgt.dgtBoard())
          )
        )
      ),
      ctx.noBot.option:
        val puzzleUrl = langHref(routes.Puzzle.home.url)
        st.section(
          linkTitle(puzzleUrl, trans.puzzles()),
          div(role := "group")(
            a(href := puzzleUrl)(trans.puzzles()),
            a(href := routes.Puzzle.dashboard(30, "home", none))(trans.puzzle.puzzleDashboard()),
            a(href := langHref(routes.Puzzle.streak))("Puzzle Streak"),
            a(href := langHref(routes.Storm.home))("Puzzle Storm"),
            a(href := langHref(routes.Racer.home))("Puzzle Racer")
          )
        )
      ,
      st.section(
        linkTitle(routes.Learn.index.url, trans.learnMenu()),
        div(role := "group")(
          ctx.noBot option frag(
            a(href := langHref(routes.Learn.index))(trans.chessBasics()),
            a(href := routes.Practice.index)(trans.practice()),
            a(href := langHref(routes.Coordinate.home))(trans.coordinates.coordinates())
          ),
          a(href := langHref(routes.Study.allDefault(1)))(trans.studyMenu()),
          ctx.kid.no option a(href := langHref(routes.Coach.all(1)))(trans.coaches()),
          canSeeClasMenu option a(href := clasRoutes.index)(trans.clas.lichessClasses())
        )
      ),
      st.section:
        val tvUrl = langHref(routes.Tv.index)
        frag(
          linkTitle(tvUrl, trans.watch()),
          div(role := "group")(
            a(href := tvUrl)("Lichess TV"),
            a(href := routes.Tv.games)(trans.currentGames()),
            (ctx.kid.no && ctx.noBot) option a(href := routes.Streamer.index())(trans.streamersMenu()),
            a(href := routes.RelayTour.index())(trans.broadcast.broadcasts()),
            ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary())
          )
        )
      ,
      st.section(
        linkTitle(routes.User.list.url, trans.community()),
        div(role := "group")(
          a(href := routes.User.list)(trans.players()),
          a(href := routes.Team.home())(trans.team.teams()),
          ctx.kid.no option a(href := routes.ForumCateg.index)(trans.forum()),
          ctx.kid.no option a(href := langHref(routes.Ublog.communityAll()))(trans.blog()),
          ctx.kid.no && ctx.me.exists(_.isPatron) option
            a(cls := "community-patron", href := routes.Plan.index)(trans.patron.donate())
        )
      ),
      st.section(
        linkTitle(routes.UserAnalysis.index.url, trans.tools()),
        div(role := "group")(
          a(href := routes.UserAnalysis.index)(trans.analysis()),
          a(href := routes.Opening.index())(trans.openings()),
          a(href := routes.Editor.index)(trans.boardEditor()),
          a(href := routes.Importer.importGame)(trans.importGame()),
          a(href := routes.Search.index())(trans.search.advancedSearch())
        )
      )
    )
