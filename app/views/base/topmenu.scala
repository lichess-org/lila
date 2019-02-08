package views.html.base

import scalatags.Text.tags2.{ nav, section }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

// #TODO rename to topnav
object topmenu {

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h2(name) else a(href := url)(name)

  def apply()(implicit ctx: Context) = nav(
    id := "topmenu",
    cls := (if (ctx.blind) "blind" else "hover")
  )(
      section(
        linkTitle("/", trans.play.frag()),
        div(role := "group")(
          if (ctx.noBot) a(href := "/?any#hook")(trans.createAGame.frag())
          else a(href := "/?any#friend")(trans.playWithAFriend.frag()),
          ctx.noBot option frag(
            a(href := routes.Tournament.home())(trans.tournament.frag()),
            a(href := routes.Simul.home)(trans.simultaneousExhibitions.frag())
          )
        )
      ),
      section(
        linkTitle(routes.Puzzle.home.toString, trans.learnMenu.frag()),
        div(role := "group")(
          ctx.noBot option frag(
            a(href := routes.Learn.index)(trans.chessBasics.frag()),
            a(href := routes.Puzzle.home)(trans.training.frag()),
            a(href := routes.Practice.index)("Practice"),
            a(href := routes.Coordinate.home)(trans.coordinates.coordinates())
          ),
          a(href := routes.Study.allDefault(1))("Study"),
          a(href := routes.Coach.allDefault(1))(trans.coaches.frag())
        )
      ),
      section(
        linkTitle(routes.Tv.index.toString, trans.watch.frag()),
        div(role := "group")(
          a(href := routes.Tv.index)("Lichess TV"),
          a(href := routes.Tv.games)(trans.currentGames.frag()),
          a(href := routes.Streamer.index())("Streamers"),
          a(href := routes.Relay.index())("Broadcasts (beta)"),
          ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary.frag())
        )
      ),
      section(
        linkTitle(routes.User.list.toString, trans.community.frag()),
        div(role := "group")(
          a(href := routes.User.list)(trans.players.frag()),
          NotForKids(frag(
            a(href := routes.Team.home())(trans.teams.frag()),
            a(href := routes.ForumCateg.index)(trans.forum.frag())
          )),
          a(href := routes.QaQuestion.index())(trans.questionsAndAnswers.frag())
        )
      ),
      section(
        linkTitle(routes.UserAnalysis.index.toString, trans.tools.frag()),
        div(role := "group")(
          a(href := routes.UserAnalysis.index)(trans.analysis.frag()),
          a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer.frag()),
          a(href := routes.Editor.index)(trans.boardEditor.frag()),
          a(href := routes.Importer.importGame)(trans.importGame.frag()),
          a(href := routes.Search.index())(trans.advancedSearch.frag())
        )
      )
    )
}
