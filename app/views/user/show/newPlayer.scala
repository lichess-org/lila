package views.html.user.show

import lila.app.templating.Environment.given
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

import controllers.routes

object newPlayer:

  def apply(u: User) =
    div(cls := "new-player")(
      h2(trans.welcomeToLichess()),
      p(
        trans.thisIsYourProfilePage(),
        u.profile.isEmpty option frag(
          br,
          trans.wouldYouLikeToX(
            a(href := routes.Account.profile)(trans.improveIt())
          )
        )
      ),
      p(
        if u.kid then trans.kidModeIsEnabled()
        else
          frag(
            trans.enabledKidModeSuggestion(
            a(href := routes.Account.kid)(trans.kidMode()),
            )
          )
      ),
      p(trans.whatNowSuggestions()),
      ul(
        li(a(href := routes.Learn.index)(trans.learnChessRules())),
        li(a(href := routes.Puzzle.home)(trans.improveWithChessTacticsPuzzles)),
        li(a(href := s"${routes.Lobby.home}#ai")(trans.playTheArtificialIntelligence())),
        li(a(href := s"${routes.Lobby.home}#hook")(trans.playOpponentsFromAroundTheWorld())),
        li(a(href := routes.User.list)(trans.ollowYourFriendsOnLichess())),
        li(a(href := routes.Tournament.home)(playInTournaments())),
        li(trans.learnFromXAndY(
          a(href := routes.Study.allDefault(1))(trans.learnFromStudies()),
          a(href := routes.Video.index)(trans.learnFromVideos())
          )
        ),
        li(a(href := routes.Pref.form("game-display"))(trans.configureLichess())),
        li(trans.exploreTheSiteAndHaveFun())
      )
    )
