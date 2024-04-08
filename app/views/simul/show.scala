package views.html.simul

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given
import lila.gathering.Condition
import lila.simul.Simul

object show:

  def apply(
      sim: Simul,
      socketVersion: lila.core.socket.SocketVersion,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      stream: Option[lila.streamer.Stream],
      verdicts: Condition.WithVerdicts
  )(using ctx: PageContext) =
    val userIsHost = ctx.userId.has(sim.hostId)
    views.html.base.layout(
      moreCss = cssTag("simul.show"),
      title = sim.fullName,
      pageModule = PageModule(
        "simul",
        Json.obj(
          "data"          -> data,
          "i18n"          -> bits.jsI18n(),
          "socketVersion" -> socketVersion,
          "userId"        -> ctx.userId,
          "chat" -> chatOption.map: c =>
            views.html.chat.json(
              c.chat,
              c.lines,
              name = trans.site.chatRoom.txt(),
              timeout = c.timeout,
              public = true,
              resourceId = lila.chat.Chat.ResourceId(s"simul/${c.chat.id}"),
              localMod = userIsHost
            ),
          "showRatings" -> ctx.pref.showRatings
        )
      ).some
    ):
      main(cls := "simul")(
        st.aside(cls := "simul__side")(
          div(cls := "simul__meta")(
            div(cls := "game-infos")(
              div(cls := "header")(
                iconTag(licon.Group),
                div(
                  span(cls := "clock")(sim.clock.config.show),
                  div(cls := "setup")(
                    sim.variants.map(_.name).mkString(", "),
                    " • ",
                    trans.site.casual(),
                    ((isGranted(_.ManageSimul) || userIsHost) && sim.isCreated).option(
                      frag(
                        " • ",
                        a(href := routes.Simul.edit(sim.id), title := "Edit simul")(iconTag(licon.Gear))
                      )
                    )
                  )
                )
              ),
              trans.site.simulHostExtraTime(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes.value),
              br,
              sim.clock.hostExtraTimePerPlayerForDisplay.map: time =>
                frag(
                  trans.site.simulHostExtraTimePerPlayer(),
                  ": ",
                  time match
                    case Left(minutes)  => pluralize("minute", minutes.value)
                    case Right(seconds) => pluralize("second", seconds.value)
                  ,
                  br
                ),
              trans.site.hostColorX(sim.color match
                case Some("white") => trans.site.white()
                case Some("black") => trans.site.black()
                case _             => trans.site.randomColor()
              ),
              sim.position
                .flatMap(p => lila.tournament.Thematic.byFen(p.opening))
                .map { pos =>
                  frag(br, a(targetBlank, href := pos.url)(pos.name))
                }
                .orElse(sim.position.map { fen =>
                  frag(
                    br,
                    "Custom position • ",
                    views.html.base.bits.fenAnalysisLink(fen)
                  )
                })
            ),
            views.html.gathering.verdicts(verdicts, sim.mainPerfType, relevant = !userIsHost) | br,
            trans.site.by(userIdLink(sim.hostId.some)),
            sim.estimatedStartAt.map: d =>
              frag(br, absClientInstant(d))
          ),
          stream.map: s =>
            views.html.streamer.bits.contextual(s.streamer.userId),
          chatOption.isDefined.option(views.html.chat.frag)
        ),
        div(cls := "simul__main box")(spinner)
      )
