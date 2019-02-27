package views.html.lobby

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "lobby__app__tabs")(span(i(cls := "ddloader"))),
    div(cls := "lobby__app__content")
  )

  def underboards(
    tours: List[lila.tournament.Tournament],
    simuls: List[lila.simul.Simul],
    leaderboard: List[lila.user.User.LightPerf],
    tournamentWinners: List[lila.tournament.Winner]
  )(implicit ctx: Context) = frag(
    div(cls := "lobby__leaderboard")(
      div(cls := "undertable_top")(
        a(cls := "more", href := routes.User.list)(trans.more(), " »"),
        span(cls := "title text", dataIcon := "C")(trans.leaderboard())
      ),
      div(cls := "undertable_inner scroll-shadow-hard")(
        table(tbody(
          leaderboard map { l =>
            tr(
              td(lightUserLink(l.user)),
              lila.rating.PerfType(l.perfKey) map { pt =>
                td(cls := "text", dataIcon := pt.iconChar)(l.rating)
              },
              td(showProgress(l.progress, withTitle = false))
            )
          }
        ))
      )
    ),
    div(cls := "lobby__winners")(
      div(cls := "undertable_top")(
        a(cls := "more", href := routes.Tournament.leaderboard)(trans.more(), " »"),
        span(cls := "title text", dataIcon := "g")(trans.tournamentWinners())
      ),
      div(cls := "undertable_inner scroll-shadow-hard")(
        table(tbody(
          tournamentWinners take 10 map { w =>
            tr(
              td(userIdLink(w.userId.some)),
              td(a(title := w.tourName, href := routes.Tournament.show(w.tourId))(scheduledTournamentNameShortHtml(w.tourName)))
            )
          }
        ))
      )
    ),
    div(cls := "lobby__tournaments")(
      div(cls := "undertable_top")(
        a(cls := "more", href := routes.Tournament.home())(frag(trans.more(), " »")),
        span(cls := "title text", dataIcon := "g")(trans.openTournaments())
      ),
      div(id := "enterable_tournaments", cls := "enterable_list undertable_inner scroll-shadow-hard")(
        views.html.tournament.enterable(tours)
      )
    ),
    div(cls := List("lobby__simuls" -> true, "none" -> simuls.isEmpty))(
      div(cls := "undertable_top")(
        a(cls := "more", href := routes.Simul.home())(frag(trans.more(), " »")),
        span(cls := "title text", dataIcon := "|")(trans.simultaneousExhibitions())
      ),
      div(id := "enterable_simuls", cls := "enterable_list undertable_inner")(
        views.html.simul.bits.allCreated(simuls)
      )
    )
  )

  def lastPosts(posts: List[lila.blog.MiniPost])(implicit ctx: Context): Option[Frag] = posts.nonEmpty option
    div(cls := "lobby__forum")(
      div(cls := "undertable_top")(
        a(cls := "more", href := routes.Blog.index())(trans.more(), " »"),
        span(cls := "title text", dataIcon := "6")(trans.latestUpdates())
      ),
      div(cls := "undertable_inner")(
        posts map { post =>
          a(cls := "post", href := routes.Blog.show(post.id, post.slug))(
            img(src := post.image),
            span(cls := "text")(
              span(cls := "title")(post.title),
              p(cls := "shortlede")(post.shortlede)
            ),
            semanticDate(post.date)
          )
        }
      )
    )

  def playbanInfo(ban: lila.playban.TempBan)(implicit ctx: Context) =
    div(cls := "lobby__playban")(
      h2("Sorry :("),
      p("We had to time you out for a ", (ban.remainingSeconds < 3600) ?? "little ", "while."),
      br,
      p("The timeout expires ", strong(secondsFromNow(ban.remainingSeconds)), "."),
      h2("Why?"),
      p(
        "We aim to provide a pleasant chess experience for everyone.", br,
        "To that effect, we must ensure that all players follow good practice.", br,
        "When a potential problem is detected, we display this message."
      ),
      h2("How to avoid this?"),
      ul(
        li("Play every game you start"),
        li("Try to win (or at least draw) every game you play"),
        li("Resign lost games (don't let the clock run down)")
      ),
      br,
      br,
      p(
        "We apologize for the temporary inconvenience,", br,
        "and wish you great games on lichess.org.", br,
        "Thank you for reading!"
      )
    )

  def currentGameInfo(current: lila.app.mashup.Preload.CurrentGame)(implicit ctx: Context) =
    div(id := "lobby_current_game")(
      h2("Hang on!"),
      p("You have a game in progress with ", strong(current.opponent), "."),
      br, br,
      a(cls := "big text button", dataIcon := "G", href := routes.Round.player(current.pov.fullId))("Join the game"),
      br, br,
      "or",
      br, br,
      form(action := routes.Round.resign(current.pov.fullId), method := "post")(
        button(cls := "big text button", dataIcon := "L")(
          if (current.pov.game.abortable) "Abort" else "Resign", " the game"
        )
      ),
      br,
      p("You can't start a new game until this one is finished."),
      br, br,
      p(
        "If you want to play several games simultaneously,",
        br,
        a(href := routes.Simul.home)("create a simultaneous exhibition event"),
        "!"
      )
    )

  def spotlight(e: lila.event.Event)(implicit ctx: Context) = a(
    href := (if (e.isNow) e.url else routes.Event.show(e.id).url),
    cls := List(
      s"tour_spotlight event_spotlight id_${e.id}" -> true,
      "invert" -> e.isNowOrSoon
    )
  )(
      i(cls := "img", dataIcon := ""),
      span(cls := "content")(
        span(cls := "name")(e.title),
        span(cls := "headline")(e.headline),
        span(cls := "more")(
          if (e.isNow) trans.eventInProgress() else momentFromNow(e.startsAt)
        )
      )
    )
}
