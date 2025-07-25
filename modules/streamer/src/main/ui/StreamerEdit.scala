package lila.streamer
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class StreamerEdit(helpers: Helpers, bits: StreamerBits):
  import helpers.{ *, given }
  import trans.streamer as trs

  def apply(s: Streamer.WithUserAndStream, form: Form[?], modZone: Option[(Frag, List[Streamer])])(using
      ctx: Context
  ) =
    val wasListed = s.streamer.approval.lastGrantedAt.isDefined
    Page(s"${s.user.titleUsername} ${trs.lichessStreamer.txt()}")
      .css("bits.streamer.form")
      .i18n(_.streamer)
      .js(
        esmInitObj(
          "bits.streamerEdit",
          "youtube" -> wasListed.so(s.streamer.youTube).so[String](_.channelId),
          "twitch" -> wasListed.so(s.streamer.twitch).so[String](_.userId)
        )
      ):
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
              val requested = s.streamer.approval.requested
              val declined = s.streamer.approval.reason.nonEmpty
              val (clas, icon) = (granted, requested, declined) match
                case (true, true, _) => ("status is-green", Icon.Search)
                case (true, false, _) => ("status is-green", Icon.Checkmark)
                case (false, true, _) => ("status is-gold", Icon.CautionTriangle)
                case (false, false, true) => ("status is-red", Icon.X)
                case (false, false, false) => ("status is", Icon.InfoCircle)
              frag(
                (ctx.is(s.user) && s.streamer.listed.value)
                  .option(
                    div(cls := clas, dataIcon := icon)(
                      if granted then
                        frag(
                          if requested then "Changes are under review." else trs.approved(),
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
                      else if requested then trs.pendingReview()
                      else if declined then
                        frag(
                          "Your previous application was declined. ",
                          s.streamer.approval.reason
                            .filter(_.trim.nonEmpty)
                            .so(r => frag(strong("Reason: "), r, br)),
                          a(href := streamerPageActivationRoute)(
                            "See instructions before submitting again"
                          )
                        )
                      else trs.pleaseFillIn()
                    )
                  ),
                ctx.is(s.user).option(div(cls := "status")(trs.streamerLanguageSettings())),
                modZone.map: (modFrag, same) =>
                  frag(
                    modFrag,
                    div(cls := "status")(
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
                    )
                  ),
                postForm(
                  cls := "form3",
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
                        form3.hidden("approval.reason", ""),
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
                              cls := "button-green",
                              name := "approval.quick",
                              value := "approve"
                            ),
                          form3.submit("Decline and next", icon = Icon.X.some)(
                            cls := "button-red",
                            name := "approval.quick",
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
                      help = trs.twitchOrYouTubeRequired().some,
                      half = true
                    )(form3.input(_)),
                    form3.group(
                      form("youTube"),
                      trs.youTubeChannelId(),
                      help = trs.twitchOrYouTubeRequired().some,
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
                    button(
                      tpe := "submit",
                      cls := "submit button text approval-request-submit",
                      title := "You must provide an image, a streamer name, and a Twitch or YouTube channel."
                    )(trs.submitForReview())
                  )
                )
              )
            }
          )
        )
