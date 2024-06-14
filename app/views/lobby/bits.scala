package views.html.lobby

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content")
  )

  def spotlights(
      events: List[lila.event.Event],
      simuls: List[lila.simul.Simul],
      tours: List[lila.tournament.Tournament]
  )(implicit ctx: Context) = {
    val max            = 3 - events.size
    val toursSelected  = lila.tournament.Spotlight.select(tours, ctx.me, max - events.size)
    val simulsSelected = simuls.take((max - toursSelected.size) atLeast 1)
    frag(
      events.map(bits.eventSpotlight),
      simulsSelected map views.html.simul.bits.homepageSpotlight,
      toursSelected map views.html.tournament.homepageSpotlight.apply
    )
  }

  def rankings(
      leaderboard: List[lila.user.User.LightPerf],
      tournamentWinners: List[lila.tournament.Winner]
  )(implicit ctx: Context) =
    frag(
      div(cls := "lobby__leaderboard lobby__box")(
        a(cls := "lobby__box__top", href := langHref(routes.User.list))(
          h2(cls := "title text", dataIcon := "'")(trans.leaderboard()),
          span(cls := "more")(trans.more(), " »")
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
      div(cls := "lobby__winners lobby__box")(
        a(cls := "lobby__box__top", href := langHref(routes.Tournament.leaderboard))(
          h2(cls := "title text", dataIcon := "g")(trans.tournamentWinners()),
          span(cls := "more")(trans.more(), " »")
        ),
        div(cls := "lobby__box__content")(
          table(
            tbody(
              tournamentWinners take 12 map { w =>
                tr(
                  td(userIdLink(w.userId.some)),
                  td(
                    a(title := w.tourName, href := langHref(routes.Tournament.show(w.tourId)))(
                      scheduledTournamentNameShortHtml(w.tourName)
                    )
                  )
                )
              }
            )
          )
        )
      )
    )

  def tournaments(
      tours: List[lila.tournament.Tournament]
  )(implicit ctx: Context) =
    div(cls := "lobby__tournaments lobby__box")(
      a(cls := "lobby__box__top", href := langHref(routes.Tournament.home))(
        h2(cls := "title text", dataIcon := "g")(trans.openTournaments()),
        span(cls := "more")(trans.more(), " »")
      ),
      div(id := "enterable_tournaments", cls := "enterable_list lobby__box__content")(
        views.html.tournament.bits.enterable(tours)
      )
    )

  def studies(
      studies: List[lila.study.Study.MiniStudy]
  )(implicit ctx: Context) =
    div(cls := "lobby__studies lobby__box")(
      a(cls := "lobby__box__top", href := langHref(routes.Study.allDefault(1)))(
        h2(cls := "title text", dataIcon := "4")(trans.studyMenu()),
        span(cls := "more")(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        views.html.study.bits.home(studies)
      )
    )

  def shogiDescription(implicit ctx: Context): Frag =
    div(cls := "lobby__description lobby__box")(
      a(cls := "lobby__box__top", href := langHref(routes.Learn.index))(
        h2(cls := "title text", dataIcon := "C")(trans.shogi()),
        span(cls := "more")(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        p(
          trans.siteDescription(),
          br,
          trans.shogiDescription(),
          br,
          trans.learnShogiHereX(strong(a(href := langHref(routes.Learn.index))(trans.shogiBasics())))
        )
      )
    )

  def forumRecent(posts: List[lila.forum.MiniForumPost])(implicit ctx: Context): Frag =
    div(cls := "lobby__forum lobby__box")(
      a(cls := "lobby__box__top", href := routes.ForumCateg.index)(
        h2(cls := "title text", dataIcon := "d")(trans.latestForumPosts()),
        span(cls := "more")(trans.more(), " »")
      ),
      ctx.noKid option div(cls := "lobby__box__content")(
        views.html.forum.post recent posts
      )
    )

  def lastPosts(posts: List[lila.blog.MiniPost])(implicit ctx: Context): Frag =
    div(cls := "lobby__blog lobby__box")(
      a(cls := "lobby__box__top", href := langHrefJP(routes.Blog.index()))(
        h2(cls := "title text", dataIcon := "6")(trans.latestUpdates()),
        span(cls := "more")(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        posts map { post =>
          a(cls     := "post", href := routes.Blog.show(post.id))(
            img(src := post.image),
            span(cls := "text")(
              strong(post.title),
              span(post.shortlede)
            ),
            semanticDate(post.date)
          )
        }
      )
    )

  def playbanInfo(ban: lila.playban.TempBan)(implicit ctx: Context) =
    nopeInfo(
      h1(trans.sorry()),
      p(trans.weHadToTimeYouOutForAWhile()),
      p(trans.timeoutExpires(strong(secondsFromNow(ban.remainingSeconds)))),
      h2(trans.why()),
      p(
        trans.pleasantShogiExperience(),
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
      p(trans.gameInProgressWithX(strong(current.opponent)), "."),
      br,
      br,
      a(cls := "text button button-fat", dataIcon := "G", href := routes.Round.player(current.pov.fullId))(
        trans.joinTheGame()
      ),
      br,
      br,
      trans.or(),
      br,
      br,
      postForm(action := routes.Round.resign(current.pov.fullId))(
        button(cls := "text button button-red", dataIcon := "L")(
          if (current.pov.game.abortable) trans.abortGame() else trans.resign()
        )
      ),
      br,
      p(trans.gameInProgressDescription())
    )

  def nopeInfo(content: Modifier*) =
    frag(
      div(cls := "lobby__app"),
      div(cls := "lobby__nope")(
        st.section(cls := "lobby__app__content")(content)
      )
    )

  private def eventSpotlight(e: lila.event.Event)(implicit ctx: Context) =
    a(
      href := (if (e.isNow) e.url else routes.Event.show(e.id).url),
      cls := List(
        s"tour-spotlight event-spotlight id_${e.id}" -> true,
        "invert"                                     -> e.isNowOrSoon
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
