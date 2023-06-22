package views.html.simul

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.common.Json.given
import lila.socket.SocketVersion
import lila.simul.Simul
import lila.gathering.Condition

object show:

  def apply(
      sim: Simul,
      socketVersion: SocketVersion,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      stream: Option[lila.streamer.Stream],
      verdicts: Condition.WithVerdicts
  )(using ctx: PageContext) =
    val userIsHost = ctx.userId has sim.hostId
    views.html.base.layout(
      moreCss = cssTag("simul.show"),
      title = sim.fullName,
      moreJs = jsModuleInit(
        "simul",
        Json.obj(
          "data"          -> data,
          "i18n"          -> bits.jsI18n(),
          "socketVersion" -> socketVersion,
          "userId"        -> ctx.userId,
          "chat" -> chatOption.map { c =>
            views.html.chat.json(
              c.chat,
              name = trans.chatRoom.txt(),
              timeout = c.timeout,
              public = true,
              resourceId = lila.chat.Chat.ResourceId(s"simul/${c.chat.id}"),
              localMod = userIsHost
            )
          },
          "showRatings" -> ctx.pref.showRatings
        )
      )
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
                    trans.casual(),
                    (isGranted(_.ManageSimul) || userIsHost) && sim.isCreated option frag(
                      " • ",
                      a(href := routes.Simul.edit(sim.id), title := "Edit simul")(iconTag(licon.Gear))
                    )
                  )
                )
              ),
              trans.simulHostExtraTime(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes.value),
              br,
              sim.clock.hostExtraTimePerPlayerForDisplay.map { time =>
                frag(
                  trans.simulHostExtraTimePerPlayer(),
                  ": ",
                  time match
                    case Left(minutes)  => pluralize("minute", minutes.value)
                    case Right(seconds) => pluralize("second", seconds.value)
                  ,
                  br
                )
              },
              trans.hostColorX(sim.color match
                case Some("white") => trans.white()
                case Some("black") => trans.black()
                case _             => trans.randomColor()
              ),
              sim.position.flatMap(p => lila.tournament.Thematic.byFen(p.opening)) map { pos =>
                frag(br, a(targetBlank, href := pos.url)(pos.name))
              } orElse sim.position.map { fen =>
                frag(
                  br,
                  "Custom position • ",
                  views.html.base.bits.fenAnalysisLink(fen)
                )
              }
            ),
            views.html.gathering.verdicts(verdicts, sim.mainPerfType, relevant = !userIsHost) | br,
            trans.by(userIdLink(sim.hostId.some)),
            sim.estimatedStartAt.map: d =>
              frag(br, absClientInstant(d))
          ),
          stream.map { s =>
            views.html.streamer.bits.contextual(s.streamer.userId)
          },
          chatOption.isDefined option views.html.chat.frag
        ),
        div(cls := "simul__main box")(spinner)
      )
