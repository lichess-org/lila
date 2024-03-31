package views.html.user.show
import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object newPlayer:

  import trans.onboarding.*

  def apply(u: User)(using Translate) =
    div(cls := "new-player")(
      h2(welcomeToLichess()),
      p(thisIsYourProfilePage()),
      p(
        if u.kid then trans.site.kidModeIsEnabled()
        else
          enabledKidModeSuggestion:
            a(href := routes.Account.kid)(trans.site.kidMode())
      ),
      p(whatNowSuggestions()),
      ul(
        li(a(href := routes.Learn.index)(learnChessRules())),
        li(a(href := routes.Puzzle.home)(improveWithChessTacticsPuzzles())),
        li(a(href := s"${routes.Lobby.home}#ai")(playTheArtificialIntelligence())),
        li(a(href := s"${routes.Lobby.home}#hook")(playOpponentsFromAroundTheWorld())),
        li(a(href := routes.User.list)(followYourFriendsOnLichess())),
        li(a(href := routes.Tournament.home)(playInTournaments())),
        li(
          learnFromXAndY(
            a(href := routes.Study.allDefault())(trans.site.toStudy()),
            a(href := routes.Video.index)(trans.learn.videos())
          )
        ),
        li(a(href := routes.Pref.form("game-display"))(configureLichess())),
        li(exploreTheSiteAndHaveFun())
      )
    )
