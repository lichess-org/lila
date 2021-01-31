package views.html.streamer

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

object edit extends Context.ToLang {

  import trans.streamer._

  def apply(
      s: lila.streamer.Streamer.WithUserAndStream,
      form: Form[_],
      modData: Option[((List[lila.mod.Modlog], List[lila.user.Note]), List[lila.streamer.Streamer])]
  )(implicit ctx: Context) = {

    views.html.base.layout(
      title = s"${s.user.titleUsername} ${lichessStreamer.txt()}",
      moreCss = cssTag("streamer.form")
    ) {
      main(cls := "page-menu")(
        bits.menu("edit", s.withoutStream.some),
        div(cls := "page-menu__content box streamer-edit")(
          if (ctx.is(s.user))
            div(cls := "streamer-header")(
              if (s.streamer.hasPicture)
                a(targetBlank, href := routes.Streamer.picture(), title := changePicture.txt())(
                  bits.pic(s.streamer, s.user)
                )
              else
                div(cls := "picture-create")(
                  ctx.is(s.user) option
                    a(targetBlank, cls := "button", href := routes.Streamer.picture())(
                      uploadPicture()
                    )
                ),
              div(cls := "overview")(
                h1(s.streamer.name),
                bits.rules
              )
            )
          else views.html.streamer.header(s),
          div(cls := "box__pad") {
            val granted = s.streamer.approval.granted
            frag(
              (ctx.is(s.user) && s.streamer.listed.value) option div(
                cls := s"status is${granted ?? "-green"}",
                dataIcon := (if (granted) "E" else "")
              )(
                if (granted)
                  frag(
                    approved(),
                    s.streamer.approval.tier > 0 option frag(
                      br,
                      strong("You have been selected for frontpage featuring!"),
                      p(
                        "Note that we can only show a limited number of streams on the homepage, ",
                        "so yours may not always appear."
                      )
                    )
                  )
                else
                  frag(
                    if (s.streamer.approval.requested) pendingReview()
                    else
                      frag(
                        if (s.streamer.completeEnough)
                          whenReady(
                            postForm(action := routes.Streamer.approvalRequest())(
                              button(tpe := "submit", cls := "button", (!ctx.is(s.user)) option disabled)(
                                requestReview()
                              )
                            )
                          )
                        else pleaseFillIn()
                      )
                  )
              ),
              ctx.is(s.user) option div(cls := "status")(
                anotherLanguage(
                  a(href := "https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes")(
                    "2-letter ISO 639-1 code"
                  )
                )
              ),
              modData.map { case ((log, notes), same) =>
                div(cls := "mod_log status")(
                  strong(cls := "text", dataIcon := "!")(
                    "Moderation history",
                    log.isEmpty option ": nothing to show."
                  ),
                  log.nonEmpty option ul(
                    log.map { e =>
                      li(
                        userIdLink(e.mod.some, withTitle = false),
                        " ",
                        b(e.showAction),
                        " ",
                        e.details,
                        " ",
                        momentFromNow(e.date)
                      )
                    }
                  ),
                  br,
                  strong(cls := "text", dataIcon := "!")(
                    "Moderator notes",
                    notes.isEmpty option ": nothing to show."
                  ),
                  notes.nonEmpty option ul(
                    notes.map { note =>
                      li(
                        p(cls := "meta")(userIdLink(note.from.some), " ", momentFromNow(note.date)),
                        p(cls := "text")(richText(note.text))
                      )
                    }
                  ),
                  br,
                  strong(cls := "text", dataIcon := "!")(
                    "Streamers with same Twitch or YouTube",
                    same.isEmpty option ": nothing to show."
                  ),
                  same.nonEmpty option table(cls := "slist")(
                    same.map { s =>
                      tr(
                        td(userIdLink(s.userId.some)),
                        td(s.name),
                        td(s.twitch.map(t => a(href := s"https://twitch.tv/${t.userId}")(t.userId))),
                        td(
                          s.youTube.map(t =>
                            a(href := s"https://youtube.com/channel/${t.channelId}")(t.channelId)
                          )
                        ),
                        td(momentFromNow(s.createdAt))
                      )
                    }
                  )
                )
              },
              postForm(
                cls := "form3",
                action := s"${routes.Streamer.edit()}${!ctx.is(s.user) ?? s"?u=${s.user.id}"}"
              )(
                isGranted(_.Streamers) option div(cls := "mod")(
                  form3.split(
                    form3.checkbox(
                      form("approval.granted"),
                      frag("Publish on the streamers list"),
                      half = true
                    ),
                    form3.checkbox(
                      form("approval.requested"),
                      frag("Active approval request"),
                      half = true
                    )
                  ),
                  form3.split(
                    form3.checkbox(
                      form("approval.chat"),
                      frag("Embed stream chat too"),
                      half = true
                    ),
                    if (granted)
                      form3.group(
                        form("approval.tier"),
                        raw("Homepage tier"),
                        help =
                          frag("Higher tier has more chance to hit homepage. Set to zero to unfeature.").some,
                        half = true
                      )(form3.select(_, lila.streamer.Streamer.tierChoices))
                    else
                      form3.checkbox(
                        form("approval.ignored"),
                        frag("Ignore further approval requests"),
                        half = true
                      )
                  ),
                  form3.actions(
                    form3
                      .submit("Approve and next")(
                        cls := "button-green",
                        name := "approval.quick",
                        value := "approve"
                      ),
                    form3.submit("Decline and next", icon = "L".some)(
                      cls := "button-red",
                      name := "approval.quick",
                      value := "decline"
                    ),
                    form3.submit(trans.apply())
                  )
                ),
                form3.globalError(form),
                form3.split(
                  form3.group(
                    form("twitch"),
                    twitchUsername(),
                    help = optionalOrEmpty().some,
                    half = true
                  )(form3.input(_)),
                  form3.group(
                    form("youTube"),
                    youtubeChannel(),
                    help = optionalOrEmpty().some,
                    half = true
                  )(form3.input(_))
                ),
                form3.split(
                  form3.group(
                    form("name"),
                    streamerName(),
                    help = keepItShort(25).some,
                    half = true
                  )(form3.input(_)),
                  form3.checkbox(
                    form("listed"),
                    visibility(),
                    help = whenApproved().some,
                    half = true
                  )
                ),
                form3.group(
                  form("headline"),
                  headline(),
                  help = tellUsAboutTheStream().some
                )(form3.input(_)),
                form3.group(form("description"), longDescription())(form3.textarea(_)(rows := 10)),
                form3.actions(
                  a(href := routes.Streamer.show(s.user.username))(trans.cancel()),
                  form3.submit(trans.apply())
                )
              )
            )
          }
        )
      )
    }
  }
}
