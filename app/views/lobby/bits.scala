package views.html.lobby

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.ublog.UblogPost

object bits {

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content")
  )

  def underboards(
      tours: List[lila.tournament.Tournament],
      simuls: List[lila.simul.Simul],
      leaderboard: List[lila.user.User.LightPerf],
      tournamentWinners: List[lila.tournament.Winner]
  )(implicit ctx: Context) =
    frag(
      ctx.pref.showRatings option div(cls := "lobby__leaderboard lobby__box")(
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := "")(trans.leaderboard()),
          a(cls := "more", href := routes.User.list)(trans.more(), " »")
        ),
        div(cls := "lobby__box__content")(
          table(
            tbody(
              leaderboard map { l =>
                tr(
                  td(lightUserLink(l.user)),
                  lila.rating.PerfType(l.perfKey) map { pt =>
                    td(cls := "text", dataIcon := pt.iconChar)(l.rating)
                  },
                  td(ratingProgress(l.progress))
                )
              }
            )
          )
        )
      ),
      div(cls := s"lobby__box ${if (ctx.pref.showRatings) "lobby__winners" else "lobby__wide-winners"}")(
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := "")(trans.tournamentWinners()),
          a(cls := "more", href := routes.Tournament.leaderboard)(trans.more(), " »")
        ),
        div(cls := "lobby__box__content")(
          table(
            tbody(
              tournamentWinners take 10 map { w =>
                tr(
                  td(userIdLink(w.userId.some)),
                  td(
                    a(title := w.tourName, href := routes.Tournament.show(w.tourId))(
                      scheduledTournamentNameShortHtml(w.tourName)
                    )
                  )
                )
              }
            )
          )
        )
      ),
      div(cls := "lobby__tournaments lobby__box")(
        a(cls := "lobby__box__top", href := routes.Tournament.home)(
          h2(cls := "title text", dataIcon := "")(trans.openTournaments()),
          span(cls := "more")(trans.more(), " »")
        ),
        div(cls := "enterable_list lobby__box__content")(
          views.html.tournament.bits.enterable(tours)
        )
      ),
      simuls.nonEmpty option div(cls := "lobby__simuls lobby__box")(
        a(cls := "lobby__box__top", href := routes.Simul.home)(
          h2(cls := "title text", dataIcon := "")(trans.simultaneousExhibitions()),
          span(cls := "more")(trans.more(), " »")
        ),
        div(cls := "enterable_list lobby__box__content")(
          views.html.simul.bits.allCreated(simuls)
        )
      )
    )

  def lastPosts(lichess: Option[lila.blog.MiniPost], uposts: List[lila.ublog.UblogPost.PreviewPost])(implicit
      ctx: Context
  ): Frag =
    div(cls := "lobby__blog ublog-post-cards")(
      lichess map { post =>
        a(cls := "ublog-post-card ublog-post-card--link", href := routes.Blog.show(post.id, post.slug))(
          img(
            src     := post.image,
            cls     := "ublog-post-card__image",
            widthA  := UblogPost.thumbnail.Small.width,
            heightA := UblogPost.thumbnail.Small.height
          ),
          span(cls := "ublog-post-card__content")(
            h2(cls := "ublog-post-card__title")(post.title),
            semanticDate(post.date)(ctx.lang)(cls := "ublog-post-card__over-image")
          )
        )
      },
      ctx.noKid option (uposts map { views.html.ublog.post.card(_, showAuthor = false, showIntro = false) })
    )

  def showUnreadLichessMessage(implicit ctx: Context) =
    nopeInfo(
      cls := "unread-lichess-message",
      p("You have received a private message from Lichess."),
      p(
        a(cls := "button button-big", href := routes.Msg.convo(lila.user.User.lichessId))(
          "Click here to read it"
        )
      )
    )

  def playbanInfo(ban: lila.playban.TempBan)(implicit ctx: Context) =
    nopeInfo(
      h1(trans.sorry()),
      p(trans.weHadToTimeYouOutForAWhile()),
      p(trans.timeoutExpires(strong(secondsFromNow(ban.remainingSeconds)))),
      h2(trans.why()),
      p(
        trans.pleasantChessExperience(),
        br,
        trans.goodPractice(),
        br,
        trans.potentialProblem()
      ),
      h2(trans.howToAvoidThis()),
      ul(
        li(trans.playEveryGame()),
        li(trans.tryToWin()),
        li(trans.resignLostGames())
      ),
      p(
        trans.temporaryInconvenience(),
        br,
        trans.wishYouGreatGames(),
        br,
        trans.thankYouForReading()
      )
    )

  def currentGameInfo(current: lila.app.mashup.Preload.CurrentGame)(implicit ctx: Context) =
    nopeInfo(
      h1(trans.hangOn()),
      p(trans.gameInProgress(strong(current.opponent))),
      br,
      br,
      a(cls := "text button button-fat", dataIcon := "", href := routes.Round.player(current.pov.fullId))(
        trans.joinTheGame()
      ),
      br,
      br,
      "or",
      br,
      br,
      postForm(action := routes.Round.resign(current.pov.fullId))(
        button(cls := "text button button-red", dataIcon := "")(
          if (current.pov.game.abortableByUser) trans.abortTheGame() else trans.resignTheGame()
        )
      ),
      br,
      p(trans.youCantStartNewGame())
    )

  def nopeInfo(content: Modifier*) =
    frag(
      div(cls := "lobby__app"),
      div(cls := "lobby__nope")(
        st.section(cls := "lobby__app__content")(content)
      )
    )

  def spotlight(e: lila.event.Event)(implicit ctx: Context) =
    a(
      href := (if (e.isNow || !e.countdown) e.url else routes.Event.show(e.id).url),
      cls := List(
        s"tour-spotlight event-spotlight id_${e.id}" -> true,
        "invert"                                     -> e.isNowOrSoon
      )
    )(
      views.html.event.iconOf(e),
      span(cls := "content")(
        span(cls := "name")(e.title),
        span(cls := "headline")(e.headline),
        span(cls := "more")(
          if (e.isNow) trans.eventInProgress() else momentFromNow(e.startsAt)
        )
      )
    )
}
