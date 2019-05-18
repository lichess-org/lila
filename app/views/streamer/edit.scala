package views.html.streamer

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.user.User

import controllers.routes

object edit {

  def apply(
    s: lila.streamer.Streamer.WithUserAndStream,
    form: Form[_],
    modData: Option[(List[lila.mod.Modlog], List[lila.user.Note])]
  )(implicit ctx: Context) = {

    val modsOnly = raw("Moderators only").some

    views.html.base.layout(
      title = s"${s.user.titleUsername} streamer page",
      moreCss = cssTag("streamer.form"),
      moreJs = jsTag("streamer.form.js")
    ) {
        main(cls := "page-menu")(
          bits.menu("edit", s.withoutStream.some),
          div(cls := "page-menu__content box streamer-edit")(
            if (ctx.is(s.user)) div(cls := "streamer-header")(
              if (s.streamer.hasPicture)
                a(target := "_blank", href := routes.Streamer.picture, title := "Change/delete your picture")(
                bits.pic(s.streamer, s.user)
              )
              else div(cls := "picture-create")(
                ctx.is(s.user) option
                  a(target := "_blank", cls := "button", href := routes.Streamer.picture)("Upload a picture")
              ),
              div(cls := "overview")(
                h1(s.streamer.name),
                bits.rules()
              )
            )
            else header(s, none),
            div(cls := "box__pad") {
              val granted = s.streamer.approval.granted
              frag(
                (ctx.is(s.user) && s.streamer.listed.value) option div(
                  cls := s"status is${granted ?? "-green"}",
                  dataIcon := (if (granted) "E" else "")
                )(
                    if (granted) frag(
                      "Your stream is approved and listed on ",
                      a(href := routes.Streamer.index())("lichess streamers list"), "."
                    )
                    else frag(
                      if (s.streamer.approval.requested) frag(
                        "Your stream is being reviewed by moderators, and will soon be listed on ",
                        a(href := routes.Streamer.index())("lichess streamers list"), "."
                      )
                      else frag(
                        if (s.streamer.completeEnough) frag(
                          "When you are ready to be listed on ",
                          a(href := routes.Streamer.index())("lichess streamers list"), ", ",
                          st.form(method := "post", action := routes.Streamer.approvalRequest)(
                            button(tpe := "submmit", cls := "button", (!ctx.is(s.user)) option disabled)(
                              "request a moderator review"
                            )
                          )
                        )
                        else "Please fill in your streamer information, and upload a picture."
                      )
                    )
                  ),
                ctx.is(s.user) option div(cls := "status")(
                  strong("If your stream is in another language than English"),
                  ", include the correct language tag (",
                  a(href := "https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes")("2-letter ISO 639-1 code"),
                  " enclosed in square brackets) at the start of your stream title. ",
                  """As examples, include "[RU]" for Russian, "[TR]" for Turkish, "[FR]" for French, etc. """,
                  "If your stream is in English, there is no need to include a language tag."
                ),
                modData.map {
                  case (log, notes) => div(cls := "mod_log status")(
                    strong(cls := "text", dataIcon := "!")(
                      "Moderation history",
                      log.isEmpty option ": nothing to show."
                    ),
                    log.nonEmpty option ul(
                      log.map { e =>
                        li(
                          userIdLink(e.mod.some, withTitle = false), " ",
                          b(e.showAction), " ", e.details, " ", momentFromNow(e.date)
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
                st.form(cls := "form3", action := s"${routes.Streamer.edit}${!ctx.is(s.user) ?? s"?u=${s.user.id}"}", method := "POST")(
                  isGranted(_.Streamers) option div(cls := "mod")(
                    form3.split(
                      form3.checkbox(form("approval.granted"), raw("Publish on the streamers list"), help = modsOnly, half = true),
                      form3.checkbox(form("approval.requested"), raw("Active approval request"), help = modsOnly, half = true)
                    ),
                    form3.split(
                      form3.checkbox(form("approval.chat"), raw("Embed stream chat too"), help = modsOnly, half = true),
                      if (granted)
                        form3.checkbox(form("approval.featured"), raw("Feature on lichess homepage"), help = modsOnly, half = true)
                      else
                        form3.checkbox(form("approval.ignored"), raw("Ignore further approval requests"), help = modsOnly, half = true)
                    ),
                    form3.action(form3.submit(trans.apply()))
                  ),
                  form3.split(
                    form3.group(form("twitch"), raw("Your Twitch username or URL"), help = raw("Optional. Leave empty if none").some, half = true)(form3.input(_)),
                    form3.group(form("youTube"), raw("Your YouTube channel ID or URL"), help = raw("Optional. Leave empty if none").some, half = true)(form3.input(_))
                  ),
                  form3.split(
                    form3.group(form("name"), raw("Your streamer name on lichess"), help = raw("Keep it short: 20 characters max").some, half = true)(form3.input(_)),
                    form3.checkbox(form("listed"), raw("Visible on the streamers page"), help = raw("When approved by moderators").some, half = true)
                  ),
                  form3.group(form("headline"), raw("Headline"), help = raw("In one sentence, tell us about your stream").some)(form3.input(_)),
                  form3.group(form("description"), raw("Long description"))(form3.textarea(_)(rows := 10)),
                  form3.actions(
                    a(href := routes.Streamer.show(s.user.username))("Cancel"),
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
