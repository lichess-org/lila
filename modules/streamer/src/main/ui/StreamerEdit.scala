package lila.streamer
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StreamerEdit(helpers: Helpers, bits: StreamerBits):
  import helpers.{ *, given }
  import trans.{ streamer as trs }

  def apply(s: Streamer.WithUserAndStream, form: Form[?], modZone: Option[(Frag, List[Streamer])])(using
      ctx: Context
  ) =
    Page(s"${s.user.titleUsername} ${trs.lichessStreamer.txt()}")
      .css("bits.streamer.form")
      .js(EsmInit("bits.streamer")):
        main(cls := "page-menu")(
          bits.menu("edit", s.some),
          div(cls := "page-menu__content box streamer-edit")(
            if ctx.is(s.user) then
              div(cls := "streamer-header")(
                div(cls := "picture-edit", attr("draggable") := "true")(
                  bits.thumbnail(s.streamer, s.user)(attr("draggable") := "true", cls := "drop-target"),
                  span(
                    label("Drag image above or"),
                    form3.file.selectImage()
                  )
                ),
                div(cls := "overview")(
                  h1(s.streamer.name),
                  bits.rules
                )
              )
            else bits.header(s, modZone.isDefined),
            div(cls := "box-pad") {
              val granted = s.streamer.approval.granted
              frag(
                (ctx.is(s.user) && s.streamer.listed.value).option(
                  div(
                    cls      := s"status is${granted.so("-green")}",
                    dataIcon := (if granted then Icon.Checkmark else Icon.InfoCircle)
                  )(
                    if granted then
                      frag(
                        trs.approved(),
                        (s.streamer.approval.tier > 0).option:
                          frag(
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
                        if s.streamer.approval.requested then trs.pendingReview()
                        else
                          frag(
                            if s.streamer.completeEnough then
                              trs.whenReady(
                                postForm(action := routes.Streamer.approvalRequest)(
                                  button(tpe := "submit", cls := "button", ctx.isnt(s.user).option(disabled))(
                                    trs.requestReview()
                                  )
                                )
                              )
                            else trs.pleaseFillIn()
                          )
                      )
                  )
                ),
                ctx.is(s.user).option(div(cls := "status")(trs.streamerLanguageSettings())),
                modZone.map: (modFrag, same) =>
                  frag(
                    modFrag,
                    br,
                    strong(cls := "text", dataIcon := Icon.CautionTriangle)(
                      "Streamers with same Twitch or YouTube",
                      same.isEmpty.option(": nothing to show.")
                    ),
                    same.nonEmpty.option(
                      table(cls := "slist")(
                        same.map: s =>
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
                      )
                    )
                  ),
                postForm(
                  cls    := "form3",
                  action := s"${routes.Streamer.edit}${ctx.isnt(s.user).so(s"?u=${s.user.id}")}"
                )(
                  Granter
                    .opt(_.Streamers)
                    .option(
                      div(cls := "mod")(
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
                          if granted then
                            form3.group(
                              form("approval.tier"),
                              raw("Homepage tier"),
                              help = frag(
                                "Higher tier has more chance to hit homepage. Set to zero to unfeature."
                              ).some,
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
                              cls   := "button-green",
                              name  := "approval.quick",
                              value := "approve"
                            ),
                          form3.submit("Decline and next", icon = Icon.X.some)(
                            cls   := "button-red",
                            name  := "approval.quick",
                            value := "decline"
                          ),
                          form3.submit(trans.site.apply())
                        )
                      )
                    ),
                  form3.globalError(form),
                  form3.split(
                    form3.group(
                      form("twitch"),
                      trs.twitchUsername(),
                      help = trs.optionalOrEmpty().some,
                      half = true
                    )(form3.input(_)),
                    form3.group(
                      form("youTube"),
                      trs.youTubeChannelId(),
                      help = trs.optionalOrEmpty().some,
                      half = true
                    )(form3.input(_))
                  ),
                  form3.split(
                    form3.group(
                      form("name"),
                      trs.streamerName(),
                      help = trs.keepItShort(25).some,
                      half = true
                    )(form3.input(_)),
                    form3.checkbox(
                      form("listed"),
                      trs.visibility(),
                      help = trs.whenApproved().some,
                      half = true
                    )
                  ),
                  form3.group(
                    form("headline"),
                    trs.headline(),
                    help = trs.tellUsAboutTheStream().some
                  )(form3.input(_)),
                  form3.group(form("description"), trs.longDescription())(form3.textarea(_)(rows := 10)),
                  form3.actions(
                    a(href := routes.Streamer.show(s.user.username))(trans.site.cancel()),
                    form3.submit(trans.site.apply())
                  )
                )
              )
            }
          )
        )
