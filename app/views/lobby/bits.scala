package views.html.lobby

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

import shogi.variant._

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
      div(cls := "lobby__leaderboard lobby__box")(
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := "'")(trans.leaderboard()),
          a(cls := "more", href := langHref(routes.User.list))(trans.more(), " »")
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
        div(cls := "lobby__box__top")(
          h2(cls := "title text", dataIcon := "g")(trans.tournamentWinners()),
          a(cls := "more", href := langHref(routes.Tournament.leaderboard))(trans.more(), " »")
        ),
        div(cls := "lobby__box__content")(
          table(
            tbody(
              tournamentWinners take 10 map { w =>
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
      ),
      div(cls := "lobby__tournaments lobby__box")(
        a(cls := "lobby__box__top", href := langHref(routes.Tournament.home))(
          h2(cls := "title text", dataIcon := "g")(trans.openTournaments()),
          span(cls := "more")(trans.more(), " »")
        ),
        div(id := "enterable_tournaments", cls := "enterable_list lobby__box__content")(
          views.html.tournament.bits.enterable(tours)
        )
      ),
      div(cls := "lobby__simuls lobby__box")(
        a(cls := "lobby__box__top", href := langHref(routes.Simul.home))(
          h2(cls := "title text", dataIcon := "f")(trans.simultaneousExhibitions()),
          span(cls := "more")(trans.more(), " »")
        ),
        div(id := "enterable_simuls", cls := "enterable_list lobby__box__content")(
          views.html.simul.bits.allCreated(simuls)
        )
      )
    )

  def shogiDescription(implicit ctx: Context): Frag =
    div(cls := "lobby__description lobby__box")(
      a(cls := "lobby__box__top", href := langHref(routes.Learn.index))(
        h2(cls := "title text", dataIcon := "C")(trans.shogi()),
        span(cls := "more")(trans.learnMenu(), " »")
      ),
      div(id := "shogi_description", cls := "lobby__box__content")(
        p(
          trans.siteDescription(),
          br,
          trans.shogiDescription(),
          br,
          trans.learnShogiHereX(strong(a(href := langHref(routes.Learn.index))(trans.shogiBasics())))
        )
      )
    )

  def variants(implicit ctx: Context): Frag =
    div(cls := "lobby__variants lobby__box")(
      a(cls := "lobby__box__top", href := routes.Page.variantHome)(
        h2(cls := "title text", dataIcon := "]")(trans.variants()),
        span(cls := "more")(trans.more(), " »")
      ),
      div(id := "variants_list", cls := "lobby__box__content")(
        Variant.all.filterNot(_.standard).map { v =>
          a(cls := "variants_item", href := routes.Page.variant(v.key, none))(variantNameTag(v))
        }
      )
    )

  private def variantNameTag(variant: Variant)(implicit ctx: Context): Frag = {
    val showKanjiName = ctx.lang.language != "ja"
    variant match {
      case Kyotoshogi =>
        h3(dataIcon := "")(
          s"${trans.kyotoshogi.txt()}${if (showKanjiName) " (京都将棋)" else ""}"
        )
      case Annanshogi =>
        h3(dataIcon := "")(
          s"${trans.annanshogi.txt()}${if (showKanjiName) " (安南将棋)" else ""}"
        )
      case Chushogi =>
        h3(dataIcon := "(")(
          s"${trans.chushogi.txt()}${if (showKanjiName) " (中将棋)" else ""}"
        )
      case Minishogi =>
        h3(dataIcon := ",")(
          s"${trans.minishogi.txt()}${if (showKanjiName) " (5五将棋)" else ""}"
        )
      case _ => trans.shogi.txt()
    }
  }

  def lastPosts(posts: List[lila.blog.MiniPost])(implicit ctx: Context): Option[Frag] = {
    posts.nonEmpty option
      div(cls := "lobby__blog lobby__box")(
        a(cls := "lobby__box__top", href := langHref(routes.Blog.index()))(
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
  }

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

  def spotlight(e: lila.event.Event)(implicit ctx: Context) =
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
