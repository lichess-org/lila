package views.html.user.show

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object newPlayer {

  def apply(u: User)(implicit lang: Lang) =
    div(cls := "new-player")(
      h2(trans.welcomeToLichess()),
      p(
        trans.thisIsYourProfilePage(),
        u.profile.isEmpty option frag(
          br,
          trans.newPlayerImproveProfile(
          a(href := routes.Account.profile)(trans.improveIt.txt()),
          )
        )
      ),
      p(
        if (u.kid) trans.kidModeIsEnabled()
        else
          frag(trans.newPlayerKidMode(
            a(href := routes.Account.kid)(trans.kidMode.txt())) 
          )
      ),
      p(
        trans.newPlayerWhatNow()
      ),
      ul(
        li(a(href := routes.Learn.index)(trans.learnChessRules())),
        li(a(href := routes.Puzzle.home)(trans.improveWithChessTactics())),
        li(a(href := s"${routes.Lobby.home}#ai")(trans.playTheAI())),
        li(a(href := s"${routes.Lobby.home}#hook")(trans.playOpponentsAroundWorld())),
        li(a(href := routes.User.list)(trans.followYourFriends())),
        li(a(href := routes.Tournament.home)(trans.playInTourns())),
        li(trans.newPlayerLearnFrom(
          a(href := routes.Study.allDefault(1))(trans.studies.txt()),
          a(href := routes.Video.index)(trans.videos.txt())
          )
        ),
        li(a(href := routes.Pref.form("game-display"))(trans.configureLichessToYourLiking())),
        li(trans.newPlayerExploreSite())
      )
    )
}
