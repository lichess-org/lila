package views.lobby

import lila.app.UiEnv.{ *, given }
import lila.rating.PerfType

object bits:

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content lpools")
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

  def recentTopics(forumTopics: List[lila.forum.RecentTopic])(using Context) =
    div(cls := "topic-list")(
      frag(
        forumTopics
          .map: topic =>
            val mostRecent = topic.posts.last
            div(
              span(
                a(
                  href  := routes.ForumCateg.show(topic.categId),
                  title := topic.categId.value.split("-").map(_.capitalize).mkString(" "),
                  cls   := s"categ-link"
                )(
                  lila.forum.ForumCateg.publicShortNames.get(topic.categId).getOrElse("TEAM")
                ),
                ": ",
                a(
                  href := routes.ForumPost.redirect(mostRecent.post.id),
                  title := s"${topic.name}\n\n${titleNameOrAnon(mostRecent.post.userId)}:  ${mostRecent.post.text}"
                )(topic.name)
              ),
              div(cls := "contributors")(
                topic.contribs match
                  case Nil => emptyFrag
                  case contribs =>
                    frag(contribs.flatMap(uid => Seq[Frag](userIdLink(uid), " · ")).dropRight(1)*),
                span(cls := "time", momentFromNow(topic.updatedAt))
              )
            )
      ),
      a(href := routes.ForumCateg.index, cls := "more")(trans.site.more(), " »")
    )
