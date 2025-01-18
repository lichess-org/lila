package lila.simul
package ui

import play.api.libs.json.*

import lila.common.Json.given
import lila.gathering.Condition.WithVerdicts
import lila.gathering.ui.GatheringUi
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SimulShow(helpers: Helpers, ui: SimulUi, gathering: GatheringUi):
  import helpers.{ *, given }

  def apply(
      sim: Simul,
      socketVersion: lila.core.socket.SocketVersion,
      data: play.api.libs.json.JsObject,
      chatOption: Option[(JsObject, Frag)],
      stream: Option[Frag],
      verdicts: WithVerdicts
  )(using ctx: Context) =
    val userIsHost = ctx.is(sim.hostId)
    Page(sim.fullName)
      .css("simul.show")
      .js(
        PageModule(
          "simul",
          Json.obj(
            "data"          -> data,
            "socketVersion" -> socketVersion,
            "userId"        -> ctx.userId,
            "chat"          -> chatOption.map(_._1),
            "showRatings"   -> ctx.pref.showRatings
          )
        )
      ):
        main(cls := "simul")(
          st.aside(cls := "simul__side")(
            div(cls := "simul__meta")(
              div(cls := "game-infos")(
                div(cls := "header")(
                  iconTag(Icon.Group),
                  div(
                    span(cls := "clock")(sim.clock.config.show),
                    div(cls := "setup")(
                      sim.variants.map(_.name).mkString(", "),
                      " • ",
                      trans.site.casual(),
                      (Granter.opt(_.ManageSimul) || userIsHost).option(
                        frag(
                          " • ",
                          a(href := routes.Simul.edit(sim.id), title := "Edit simul")(iconTag(Icon.Gear))
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
                  case _             => trans.site.randomColor()),
                sim.position
                  .flatMap(p => lila.gathering.Thematic.byFen(p.opening))
                  .map { pos =>
                    frag(br, a(targetBlank, href := pos.url)(pos.name))
                  }
                  .orElse(sim.position.map { fen =>
                    frag(
                      br,
                      "Custom position • ",
                      lila.ui.bits.fenAnalysisLink(fen)
                    )
                  })
              ),
              gathering.verdicts(verdicts, sim.mainPerfType, relevant = !userIsHost) | br,
              trans.site.by(userIdLink(sim.hostId.some)),
              sim.estimatedStartAt.map: d =>
                frag(br, absClientInstant(d))
            ),
            stream,
            chatOption.map(_._2)
          ),
          div(cls := "simul__main box")(spinner)
        )
