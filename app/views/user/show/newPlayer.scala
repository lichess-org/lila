package views.html.user.show

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import play.api.i18n.Lang

import controllers.routes

object newPlayer {

  def apply(u: User)(implicit lang: Lang) =
    div(cls := "new-player")(
      h2(trans.welcomeToX("lishogi.org")),
      p(
        trans.thisIsProfilePage(),
        u.profile.isEmpty option frag(
          br,
          a(href := routes.Account.profile)(trans.editProfile())
        )
      ),
      p(
        frag(
          trans.whatNow(),
          br,
          trans.hereAreSuggestions()
        )
      ),
      ul(
        li(a(href := routes.Learn.index)(trans.learn.learnShogi())),
        li(a(href := routes.Puzzle.home)(trans.puzzleDesc())),
        li(a(href := s"${routes.Lobby.home}#ai")(trans.playWithTheMachine())),
        li(a(href := s"${routes.Lobby.home}#hook")(trans.playShogiInStyle())),
        li(a(href := routes.User.list)(trans.findFriends())),
        li(a(href := routes.Tournament.home)(trans.tournaments())),
        li(a(href := routes.Study.allDefault(1))(trans.studyMenu())),
        li(a(href := routes.Pref.form("game-display"))(trans.preferences.preferences())),
        li("Explore the site and have fun :)")
      )
    )
}
