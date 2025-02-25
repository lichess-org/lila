package lila.streamer
package ui
import lila.core.id.{ CmsPageKey, ImageId }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StreamerBits(helpers: Helpers)(picfitUrl: lila.core.misc.PicfitUrl):
  import helpers.{ *, given }
  import trans.streamer as trs

  def create(using Context) =
    postForm(cls := "streamer-new", action := routes.Streamer.create)(
      h2(trans.streamer.doYouHaveStream()),
      br,
      br,
      rules,
      br,
      br,
      p(style := "text-align: center")(
        submitButton(cls := "button button-fat text", dataIcon := Icon.Mic)(trans.streamer.hereWeGo())
      )
    )

  def header(s: Streamer.WithUserAndStream, modView: Boolean = false)(using ctx: Context) =
    val isMe  = ctx.is(s.streamer)
    val isMod = Granter.opt(_.ModLog)
    div(cls := "streamer-header")(
      thumbnail(s.streamer, s.user),
      div(cls := "overview")(
        streamerTitle(s),
        s.streamer.headline.map(_.value).map { d =>
          val clsName = if d.length < 60 then "small" else if d.length < 120 then "medium" else "large"
          p(cls := s"headline $clsName")(d)
        },
        ul(cls := "services")(
          s.streamer.twitch.map { twitch =>
            li(
              a(
                cls := List(
                  "service twitch" -> true,
                  "live"           -> s.stream.exists(_.twitch)
                ),
                href := twitch.fullUrl
              )(twitch.minUrl)
            )
          },
          s.streamer.youTube.map { youTube =>
            li(
              a(
                cls := List(
                  "service youTube" -> true,
                  "live"            -> s.stream.exists(_.youTube)
                ),
                href := youTube.fullUrl
              )(youTube.minUrl)
            )
          }
        ),
        span(
          div(cls := "ats")(
            s.stream
              .map { s =>
                p(cls := "at")(trs.currentlyStreaming(strong(s.status)))
              }
              .getOrElse(
                frag(
                  p(cls := "at")(trans.site.lastSeenActive(momentFromNow(s.streamer.seenAt))),
                  s.streamer.liveAt.map { liveAt =>
                    p(cls := "at")(trs.lastStream(momentFromNow(liveAt)))
                  }
                )
              )
          ),
          (s.streamer.youTube.isDefined && s.stream.isEmpty && (isMe || isMod)).option(
            form(
              action := routes.Streamer.checkOnline(s.streamer.userId).url,
              method := "post"
            )(input(cls := "button online-check", tpe := "submit", value := "force online check"))
          )
        ),
        div(cls := "streamer-footer")(
          (!modView).option(subscribeButtonFor(s)),
          streamerProfile(s)
        )
      )
    )

  object thumbnail:
    def apply(s: Streamer, u: User) =
      img(
        widthA  := Streamer.imageSize,
        heightA := Streamer.imageSize,
        cls     := "picture",
        src     := url(s),
        alt     := s"${u.titleUsername} Lichess streamer picture"
      )
    def url(s: Streamer) =
      s.picture match
        case Some(image) => picfitUrl.thumbnail(image, Streamer.imageSize, Streamer.imageSize)
        case _           => assetUrl("images/placeholder.png")

  def menu(active: String, s: Option[Streamer.WithContext])(using ctx: Context) =
    lila.ui.bits.subnav(
      a(cls := active.active("index"), href := routes.Streamer.index())(trs.allStreamers()),
      s.map { st =>
        frag(
          a(cls := active.active("show"), href := routes.Streamer.show(st.streamer.userId))(
            st.streamer.name
          ),
          (ctx.is(st.user) || Granter.opt(_.Streamers)).option(
            a(cls := active.active("edit"), href := s"${routes.Streamer.edit}?u=${st.streamer.id.value}")(
              trs.editPage()
            )
          )
        )
      }.getOrElse(a(href := routes.Streamer.edit)(trs.yourPage())),
      Granter
        .opt(_.Streamers)
        .option(
          a(
            cls  := active.active("requests"),
            href := s"${routes.Streamer.index()}?requests=1"
          )("Approval requests")
        ),
      a(
        dataIcon := Icon.InfoCircle,
        cls      := "text",
        href     := "/blog/Wk5z0R8AACMf6ZwN/join-the-lichess-streamer-community"
      )(
        "Streamer community"
      ),
      a(href := "/about")(trs.downloadKit())
    )

  def redirectLink(username: UserStr, isStreaming: Option[Boolean] = None): Tag =
    isStreaming match
      case Some(false) => a(href := routes.Streamer.show(username))
      case _           => a(href := routes.Streamer.redirect(username), targetBlank, noFollow)

  def liveStreams(l: LiveStreams.WithTitles): Frag =
    l.live.streams.map { s =>
      redirectLink(s.streamer.id.into(UserStr))(
        cls   := "stream highlight",
        title := s.status
      )(
        strong(cls := "text", dataIcon := Icon.Mic)(l.titleName(s)),
        " ",
        s.cleanStatus
      )
    }

  def contextual(streamers: List[UserId])(using Translate): Option[Tag] =
    streamers.nonEmpty.option:
      div(cls := "context-streamers")(streamers.map(contextual))

  def contextual(userId: UserId)(using Translate): Tag =
    redirectLink(userId)(cls := "context-streamer text", dataIcon := Icon.Mic):
      trs.xIsStreaming(strong(titleNameOrId(userId)))

  def rules(using Translate) =
    ul(cls := "streamer-rules")(
      h2(trans.streamer.rules()),
      ul(
        li(trs.rule1()),
        li(trs.rule2()),
        li(
          trs.rule4(
            a(href := routes.Cms.lonePage(CmsPageKey("streaming-fairplay-faq")))(trs.streamingFairplayFAQ())
          )
        ),
        li(a(href := streamerPageActivationRoute.url)(trs.rule3()))
      ),
      h2(trs.perks()),
      ul(
        li(trs.perk1()),
        li(trs.perk2()),
        li(trs.perk3()),
        li(trs.perk4())
      )
    )

  def streamerTitle(s: Streamer.WithContext) =
    span(cls := "streamer-title")(
      h1(dataIcon := Icon.Mic)(titleTag(s.user.title), s.streamer.name),
      s.streamer.lastStreamLang.map: language =>
        span(cls := "streamer-lang")(langList.nameByLanguage(language))
    )

  def subscribeButtonFor(s: Streamer.WithContext)(using ctx: Context): Option[Tag] =
    (ctx.isAuth && ctx.isnt(s.user)).option:
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
        trans.site.subscribe()
      )

  def streamerProfile(s: Streamer.WithContext)(using Translate) =
    span(cls := "streamer-profile")(userLink(s.user))
