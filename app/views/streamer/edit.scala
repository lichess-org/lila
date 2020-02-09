package views.html.streamer

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object edit extends Context.ToLang {

  import trans.streamer._

  def apply(
      s: lila.streamer.Streamer.WithUserAndStream,
      form: Form[_],
      modData: Option[(List[lila.mod.Modlog], List[lila.user.Note])]
  )(implicit ctx: Context) = {

    val modsOnly = raw("Moderators only").some

    views.html.base.layout(
      title = s"${s.user.titleUsername} ${lichessStreamer.txt()}",
      moreCss = cssTag("streamer.form"),
      moreJs = jsTag("streamer.form.js")
    ) {
      main(cls := "page-menu")(
        bits.menu("edit", s.withoutStream.some),
        div(cls := "page-menu__content box streamer-edit")(
          if (ctx.is(s.user))
            div(cls := "streamer-header")(
              if (s.streamer.hasPicture)
                a(target := "_blank", href := routes.Streamer.picture, title := changePicture.txt())(
                  bits.pic(s.streamer, s.user)
                )
              else
                div(cls := "picture-create")(
                  ctx.is(s.user) option
                    a(target := "_blank", cls := "button", href := routes.Streamer.picture)(
                      uploadPicture()
                    )
                ),
              div(cls := "overview")(
                h1(s.streamer.name),
                bits.rules
              )
            )
          else views.html.streamer.header(s, none),
          div(cls := "box__pad") {
            val granted = s.streamer.approval.granted
            frag(
              (ctx.is(s.user) && s.streamer.listed.value) option div(
                cls := s"status is${granted ?? "-green"}",
                dataIcon := (if (granted) "E" else "")
              )(
                if (granted) approved()
                else
                  frag(
                    if (s.streamer.approval.requested) pendingReview()
                    else
                      frag(
                        if (s.streamer.completeEnough)
                          whenReady(
                            postForm(action := routes.Streamer.approvalRequest)(
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
              modData.map {
                case (log, notes) =>
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
                    )
                  )
              },
              postForm(
                cls := "form3",
                action := s"${routes.Streamer.edit}${!ctx.is(s.user) ?? s"?u=${s.user.id}"}"
              )(
                isGranted(_.Streamers) option div(cls := "mod")(
                  form3.split(
                    form3.checkbox(
                      form("approval.granted"),
                      frag("Publish on the streamers list"),
                      help = modsOnly,
                      half = true
                    ),
                    form3.checkbox(
                      form("approval.requested"),
                      frag("Active approval request"),
                      help = modsOnly,
                      half = true
                    )
                  ),
                  form3.split(
                    form3.checkbox(
                      form("approval.chat"),
                      frag("Embed stream chat too"),
                      help = modsOnly,
                      half = true
                    ),
                    if (granted)
                      form3.checkbox(
                        form("approval.featured"),
                        frag("Feature on lichess homepage"),
                        help = modsOnly,
                        half = true
                      )
                    else
                      form3.checkbox(
                        form("approval.ignored"),
                        frag("Ignore further approval requests"),
                        help = modsOnly,
                        half = true
                      )
                  ),
                  form3.action(form3.submit(trans.apply()))
                ),
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
                    help = keepItShort(20).some,
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
