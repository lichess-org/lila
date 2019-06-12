package views.html.base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object topnav {

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  def apply()(implicit ctx: Context) = st.nav(id := "topnav", role := "navigation", cls := "hover")(
    st.section(
      linkTitle("/", frag(
        span(cls := "play")(trans.play()),
        span(cls := "home")("lichess.org")
      )),
      div(role := "group")(
        if (ctx.noBot) a(href := "/?any#hook")(trans.createAGame())
        else a(href := "/?any#friend")(trans.playWithAFriend()),
        ctx.noBot option frag(
          a(href := routes.Tournament.home())(trans.tournaments()),
          a(href := routes.Simul.home)(trans.simultaneousExhibitions())
        )
      )
    ),
    st.section(
      linkTitle(routes.Puzzle.home.toString, trans.learnMenu()),
      div(role := "group")(
        ctx.noBot option frag(
          a(href := routes.Learn.index)(trans.chessBasics()),
          a(href := routes.Puzzle.home)(trans.training()),
          a(href := routes.Practice.index)("Practice"),
          a(href := routes.Coordinate.home)(trans.coordinates.coordinates())
        ),
        a(href := routes.Study.allDefault(1))("Study"),
        a(href := routes.Coach.allDefault(1))(trans.coaches())
      )
    ),
    st.section(
      linkTitle(routes.Tv.index.toString, trans.watch()),
      div(role := "group")(
        a(href := routes.Tv.index)("Lichess TV"),
        a(href := routes.Tv.games)(trans.currentGames()),
        a(href := routes.Streamer.index())("Streamers"),
        a(href := routes.Relay.index())("Broadcasts"),
        ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary())
      )
    ),
    st.section(
      linkTitle(routes.User.list.toString, trans.community()),
      div(role := "group")(
        a(href := routes.User.list)(trans.players()),
        NotForKids(frag(
          a(href := routes.Team.home())(trans.teams()),
          a(href := routes.ForumCateg.index)(trans.forum())
        ))
      )
    ),
    st.section(
      linkTitle(routes.UserAnalysis.index.toString, trans.tools()),
      div(role := "group")(
        a(href := routes.UserAnalysis.index)(trans.analysis()),
        a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer()),
        a(href := routes.Editor.index)(trans.boardEditor()),
        a(href := routes.Importer.importGame)(trans.importGame()),
        a(href := routes.Search.index())(trans.advancedSearch())
      )
    )
  )
}
