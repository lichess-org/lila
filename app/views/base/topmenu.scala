package views.html.base

import scalatags.Text.tags2.section

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object topmenu {

  def apply()(implicit ctx: Context) = div(id := "topmenu", cls := "hover")(
    section(
      a(href := "/")(trans.play()),
      div(
        if (ctx.noBot) a(href := "/?any#hook")(trans.createAGame())
        else a(href := "/?any#friend")(trans.playWithAFriend()),
        ctx.noBot option frag(
          a(href := routes.Tournament.home())(trans.tournament()),
          a(href := routes.Simul.home)(trans.simultaneousExhibitions())
        )
      )
    ),
    section(
      a(href := routes.Puzzle.home)(trans.learnMenu()),
      div(
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
    section(
      a(href := routes.Tv.index)(trans.watch()),
      div(
        a(href := routes.Tv.index)("Lichess TV"),
        a(href := routes.Tv.games)(trans.currentGames()),
        a(href := routes.Streamer.index())("Streamers"),
        a(href := routes.Relay.index())("Broadcasts (beta)"),
        ctx.noBot option a(href := routes.Video.index)(trans.videoLibrary())
      )
    ),
    section(
      a(href := routes.User.list)(trans.community()),
      div(
        a(href := routes.User.list)(trans.players()),
        NotForKids(frag(
          a(href := routes.Team.home())(trans.teams()),
          a(href := routes.ForumCateg.index)(trans.forum())
        )),
        a(href := routes.QaQuestion.index())(trans.questionsAndAnswers())
      )
    ),
    section(
      a(href := routes.UserAnalysis.index)(trans.tools()),
      div(
        a(href := routes.UserAnalysis.index)(trans.analysis()),
        a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer()),
        a(href := routes.Editor.index)(trans.boardEditor()),
        a(href := routes.Importer.importGame)(trans.importGame()),
        a(href := routes.Search.index())(trans.advancedSearch())
      )
    )
  )
}
