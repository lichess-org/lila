package views.lobby

import lila.app.UiEnv.{ *, given }
import lila.rating.PerfType

object bits:

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content lpools")
  )

  def underboards(
      tours: List[lila.tournament.Tournament],
      simuls: List[lila.simul.Simul],
      leaderboard: List[lila.core.user.LightPerf],
      tournamentWinners: List[lila.tournament.Winner]
  )(using ctx: Context) =
    frag(
      ctx.pref.showRatings.option(
        div(cls := "lobby__leaderboard lobby__box")(
          div(cls := "lobby__box__top")(
            h2(cls := "title text", dataIcon := Icon.CrownElite)(trans.site.leaderboard()),
            a(cls := "more", href := routes.User.list)(trans.site.more(), " »")
          ),
          div(cls := "lobby__box__content"):
            table:
              tbody:
                leaderboard.map: l =>
                  tr(
                    td(lightUserLink(l.user)),
                    td(cls := "text", dataIcon := PerfType(l.perfKey).icon)(l.rating),
                    td(ratingProgress(l.progress))
                  )
        )
      ),
      div(cls := s"lobby__box ${if ctx.pref.showRatings then "lobby__winners" else "lobby__wide-winners"}")(
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := Icon.Trophy)(trans.arena.tournamentWinners()),
          a(cls := "more", href := routes.Tournament.leaderboard)(trans.site.more(), " »")
        ),
        div(cls := "lobby__box__content"):
          table:
            tbody:
              tournamentWinners
                .take(10)
                .map: w =>
                  tr(
                    td(userIdLink(w.userId.some)),
                    td:
                      a(title := w.tourName, href := routes.Tournament.show(w.tourId)):
                        views.tournament.ui.scheduledTournamentNameShortHtml(w.tourName)
                  )
      ),
      div(cls := "lobby__tournaments-simuls")(
        div(cls := "lobby__tournaments lobby__box")(
          a(cls := "lobby__box__top", href := routes.Tournament.home)(
            h2(cls := "title text", dataIcon := Icon.Trophy)(trans.site.openTournaments()),
            span(cls := "more")(trans.site.more(), " »")
          ),
          div(cls := "lobby__box__content"):
            views.tournament.ui.enterable(tours)
        ),
        simuls.nonEmpty.option(
          div(cls := "lobby__simuls lobby__box")(
            a(cls := "lobby__box__top", href := routes.Simul.home)(
              h2(cls := "title text", dataIcon := Icon.Group)(trans.site.simultaneousExhibitions()),
              span(cls := "more")(trans.site.more(), " »")
            ),
            div(cls := "lobby__box__content"):
              views.simul.ui.allCreated(simuls, withName = false)
          )
        )
      )
    )

  def showUnreadLichessMessage(using Context) =
    nopeInfo(
      cls := "unread-lichess-message",
      p(trans.site.showUnreadLichessMessage()),
      p:
        a(cls := "button button-big", href := routes.Msg.convo(UserId.lichess)):
          trans.site.clickHereToReadIt()
    )

  def playbanInfo(ban: lila.playban.TempBan)(using Context) =
    nopeInfo(
      h1(trans.site.sorry()),
      p(trans.site.weHadToTimeYouOutForAWhile()),
      p(strong(timeRemaining(ban.endsAt))),
      h2(trans.site.why()),
      p(
        trans.site.pleasantChessExperience(),
        br,
        trans.site.goodPractice(),
        br,
        trans.site.potentialProblem()
      ),
      h2(trans.site.howToAvoidThis()),
      ul(
        li(trans.site.playEveryGame()),
        li(trans.site.tryToWin()),
        li(trans.site.resignLostGames())
      ),
      p(
        trans.site.temporaryInconvenience(),
        br,
        trans.site.wishYouGreatGames(),
        br,
        trans.site.thankYouForReading()
      )
    )

  def currentGameInfo(current: lila.app.mashup.Preload.CurrentGame)(using Context) =
    nopeInfo(
      h1(trans.site.hangOn()),
      p(trans.site.gameInProgress(strong(current.opponent))),
      br,
      br,
      a(
        cls      := "text button button-fat",
        dataIcon := Icon.PlayTriangle,
        href     := routes.Round.player(current.pov.fullId)
      )(
        trans.site.joinTheGame()
      ),
      br,
      br,
      "or",
      br,
      br,
      postForm(action := routes.Round.resign(current.pov.fullId))(
        button(cls := "text button button-red", dataIcon := Icon.X):
          if current.pov.game.abortableByUser then trans.site.abortTheGame() else trans.site.resignTheGame()
      ),
      br,
      p(trans.site.youCantStartNewGame())
    )

  def nopeInfo(content: Modifier*) =
    frag(
      div(cls := "lobby__app"),
      div(cls := "lobby__nope"):
        st.section(cls := "lobby__app__content")(content)
    )

  def spotlight(e: lila.event.Event)(using Context) =
    a(
      href := (if e.isNow || !e.countdown then e.url else routes.Event.show(e.id).url),
      cls := List(
        s"tour-spotlight event-spotlight id_${e.id}" -> true,
        "invert"                                     -> e.isNowOrSoon
      )
    )(
      views.event.iconOf(e),
      span(cls := "content")(
        span(cls := "name")(e.title),
        span(cls := "headline")(e.headline),
        span(cls := "more"):
          if e.isNow then trans.site.eventInProgress() else momentFromNow(e.startsAt)
      )
    )
