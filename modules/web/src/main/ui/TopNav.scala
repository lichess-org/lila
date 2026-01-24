package lila.web
package ui

import scalalib.model.Days
import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TopNav(helpers: Helpers):
  import helpers.{ *, given }

  private def linkTitle(url: String, name: Frag)(using ctx: Context) =
    if ctx.blind then h3(name) else a(href := url)(name)

  def apply(hasClas: Boolean, hasDgt: Boolean)(using ctx: Context) =
    val canSeeClasMenu = hasClas || ctx.me.exists(u => u.hasTitle || u.roles.contains("ROLE_COACH"))
    st.nav(id := "topnav", cls := "hover")(
      st.section(
        linkTitle(
          "/",
          frag(
            span(cls := "play")(trans.site.play()),
            span(cls := "home")("lichess.org")
          )
        ),
        div(role := "group")(
          if ctx.noBot then a(href := s"${langHref("/")}?any#hook")(trans.site.createLobbyGame())
          else a(href := "/?any#friend")(trans.site.challengeAFriend()),
          Option.when(ctx.noBot):
            frag(
              a(href := langHref(routes.Tournament.home))(trans.arena.arenaTournaments()),
              a(href := langHref(routes.Swiss.home))(trans.swiss.swissTournaments()),
              a(href := langHref(routes.Simul.home))(trans.site.simultaneousExhibitions()),
              hasDgt.option(a(href := routes.DgtCtrl.index)(trans.dgt.dgtBoard())),
              (ctx.kid.no && !ctx.me.exists(_.isPatron)).option:
                a(cls := "community-patron mobile-only", href := routes.Plan.index())(trans.patron.donate())
            )
        )
      ),
      Option.when(ctx.noBot):
        val puzzleUrl = langHref(routes.Puzzle.home.url)
        st.section(
          linkTitle(puzzleUrl, trans.site.puzzles()),
          div(role := "group")(
            a(href := puzzleUrl)(trans.site.puzzles()),
            a(href := langHref(routes.Puzzle.themes))(trans.puzzle.puzzleThemes()),
            a(href := routes.Puzzle.dashboard(Days(30), "home", none))(trans.puzzle.puzzleDashboard()),
            a(href := langHref(routes.Puzzle.streak))("Puzzle Streak"),
            a(href := langHref(routes.Storm.home))("Puzzle Storm"),
            a(href := langHref(routes.Racer.home))("Puzzle Racer")
          )
        )
      ,
      st.section(
        linkTitle(routes.Learn.index.url, trans.site.learnMenu()),
        div(role := "group")(
          Option.when(ctx.noBot):
            frag(
              a(href := langHref(routes.Learn.index))(trans.site.chessBasics()),
              a(href := routes.Practice.index)(trans.site.practice()),
              a(href := langHref(routes.Coordinate.home))(trans.coordinates.coordinates())
            )
          ,
          a(href := langHref(routes.Study.allDefault()))(trans.site.studyMenu()),
          ctx.kid.no.option(a(href := langHref(routes.Coach.all(1)))(trans.site.coaches())),
          canSeeClasMenu.option(a(href := routes.Clas.index)(trans.clas.lichessClasses()))
        )
      ),
      st.section:
        val tvUrl = langHref(routes.Tv.index)
        frag(
          linkTitle(tvUrl, trans.site.watch()),
          div(role := "group")(
            a(href := routes.RelayTour.index())(trans.broadcast.broadcasts()),
            a(href := langHref(routes.Tv.index))("Lichess TV"),
            a(href := routes.Tv.games)(trans.site.currentGames()),
            (ctx.kid.no && ctx.noBot).option(a(href := routes.Streamer.index())(trans.site.streamersMenu())),
            ctx.noBot.option(a(href := langHref(routes.Video.index))(trans.site.videoLibrary()))
          )
        )
      ,
      st.section(
        linkTitle(routes.User.list.url, trans.site.community()),
        div(role := "group")(
          a(href := routes.User.list)(trans.site.players()),
          ctx.me.map(me => a(href := routes.Relation.following(me.username))(trans.site.friends())),
          a(href := routes.Team.home())(trans.team.teams()),
          ctx.kid.no.option(a(href := routes.ForumCateg.index)(trans.site.forum())),
          ctx.kid.no.option(a(href := langHref(routes.Ublog.communityAll()))(trans.site.blog())),
          (ctx.kid.no && ctx.me.exists(_.isPatron))
            .option(a(cls := "community-patron", href := routes.Plan.index())(trans.patron.donate()))
        )
      ),
      st.section(
        linkTitle(routes.UserAnalysis.index.url, trans.site.tools()),
        div(role := "group")(
          a(href := routes.UserAnalysis.index)(trans.site.analysis()),
          a(href := routes.Opening.index())(trans.site.openings()),
          a(href := routes.Editor.index)(trans.site.boardEditor()),
          a(href := routes.Importer.importGame)(trans.site.importGame()),
          a(href := routes.Search.index())(trans.search.advancedSearch())
        )
      ),
      st.section:
        val broadcastUrl = langHref(routes.RelayTour.index())
        frag(
          linkTitle(broadcastUrl, "Tournaments"),
          div(role := "group")(
            a(href := routes.RelayTour.index())(trans.broadcast.broadcasts()),
            a(href := langHref(routes.Tournament.home))(trans.arena.arenaTournaments()),
            a(href := langHref(routes.Swiss.home))(trans.swiss.swissTournaments()),
            a(href := langHref(routes.Simul.home))(trans.site.simultaneousExhibitions())
          )
        )
    )
