package views.html.streamer

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.LangList

object bits:

  import trans.streamer.*

  def create(using PageContext) =
    views.html.site.message(
      title = becomeStreamer.txt(),
      icon = Some(licon.Mic),
      moreCss = cssTag("streamer.form").some
    )(
      postForm(cls := "streamer-new", action := routes.Streamer.create)(
        h2(doYouHaveStream()),
        br,
        br,
        bits.rules,
        br,
        br,
        p(style := "text-align: center")(
          submitButton(cls := "button button-fat text", dataIcon := licon.Mic)(hereWeGo())
        )
      )
    )

  def menu(active: String, s: Option[lila.streamer.Streamer.WithContext])(using ctx: PageContext) =
    views.html.site.bits.subnav(
      a(cls := active.active("index"), href := routes.Streamer.index())(allStreamers()),
      s.map { st =>
        frag(
          a(cls := active.active("show"), href := routes.Streamer.show(st.streamer.id.value))(
            st.streamer.name
          ),
          (ctx.is(st.user) || isGranted(_.Streamers)) option
            a(cls := active.active("edit"), href := s"${routes.Streamer.edit}?u=${st.streamer.id.value}")(
              editPage()
            )
        )
      } getOrElse a(href := routes.Streamer.edit)(yourPage()),
      isGranted(_.Streamers) option a(
        cls  := active.active("requests"),
        href := s"${routes.Streamer.index()}?requests=1"
      )("Approval requests"),
      a(
        dataIcon := licon.InfoCircle,
        cls      := "text",
        href     := "/blog/Wk5z0R8AACMf6ZwN/join-the-lichess-streamer-community"
      )(
        "Streamer community"
      ),
      a(href := "/about")(downloadKit())
    )

  def redirectLink(username: UserStr, isStreaming: Option[Boolean] = None) =
    isStreaming match
      case Some(false) => a(href := routes.Streamer.show(username.value))
      case _           => a(href := routes.Streamer.redirect(username.value), targetBlank, noFollow)

  def liveStreams(l: lila.streamer.LiveStreams.WithTitles): Frag =
    l.live.streams.map { s =>
      redirectLink(s.streamer.id into UserStr)(
        cls   := "stream highlight",
        title := s.status
      )(
        strong(cls := "text", dataIcon := licon.Mic)(l titleName s),
        " ",
        s.cleanStatus
      )
    }

  def contextual(streamers: List[UserId])(using Lang): Option[Tag] =
    streamers.nonEmpty option div(cls := "context-streamers"):
      streamers map contextual

  def contextual(userId: UserId)(using Lang): Tag =
    redirectLink(userId)(cls := "context-streamer text", dataIcon := licon.Mic):
      xIsStreaming(strong(titleNameOrId(userId)))

  def rules(using Lang) =
    ul(cls := "streamer-rules")(
      h2(trans.streamer.rules()),
      ul(
        li(rule1()),
        li(rule2()),
        li(
          rule4(a(href := routes.ContentPage.loneBookmark("streaming-fairplay-faq"))(streamingFairplayFAQ()))
        ),
        li(a(href := routes.ContentPage.loneBookmark("streamer-page-activation"))(rule3()))
      ),
      h2(perks()),
      ul(
        li(perk1()),
        li(perk2()),
        li(perk3()),
        li(perk4())
      )
    )

  def streamerTitle(s: lila.streamer.Streamer.WithContext) =
    span(cls := "streamer-title")(
      h1(dataIcon := licon.Mic)(titleTag(s.user.title), s.streamer.name),
      s.streamer.lastStreamLang.map: language =>
        span(cls := "streamer-lang")(LangList nameByStr language)
    )

  def subscribeButtonFor(s: lila.streamer.Streamer.WithContext)(using ctx: PageContext): Option[Tag] =
    ctx.isAuth && !ctx.is(s.user) option {
      val id = s"streamer-subscribe-${s.streamer.userId}"
      label(cls := "streamer-subscribe")(
        `for`          := id,
        data("action") := s"${routes.Streamer.subscribe(s.streamer.userId, !s.subscribed)}"
      )(
        span(
          form3.cmnToggle(
            fieldId = id,
            fieldName = id,
            checked = s.subscribed
          )
        ),
        trans.subscribe()
      )
    }
  def streamerProfile(s: lila.streamer.Streamer.WithContext)(using Lang) =
    span(cls := "streamer-profile")(userLink(s.user))
