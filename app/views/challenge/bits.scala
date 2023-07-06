package views.html.challenge

import play.api.libs.json.{ Json, JsObject }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.challenge.Challenge
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits:

  def js(c: Challenge, json: JsObject, owner: Boolean, color: Option[chess.Color] = None)(using PageContext) =
    jsModuleInit(
      "challengePage",
      Json.obj(
        "socketUrl" -> s"/challenge/${c.id}/socket/v$apiVersion",
        "xhrUrl"    -> routes.Challenge.show(c.id, color.map(_.name)).url,
        "owner"     -> owner,
        "data"      -> json
      )
    )

  def details(c: Challenge, requestedColor: Option[chess.Color])(using ctx: PageContext) =
    div(cls := "details")(
      div(
        cls      := "variant",
        dataIcon := (if c.initialFen.isDefined then licon.Feather else c.perfType.icon)
      )(
        div(
          views.html.game.bits.variantLink(c.variant, c.perfType, c.initialFen),
          br,
          span(cls := "clock"):
            c.daysPerTurn
              .fold(shortClockName(c.clock.map(_.config))): days =>
                if days.value == 1 then trans.oneDay()
                else trans.nbDays.pluralSame(days.value)
        )
      ),
      div(cls := "mode")(
        c.open.fold(c.colorChoice.some)(_.colorFor(requestedColor)) map { colorChoice =>
          frag(colorChoice.trans(), " • ")
        },
        modeName(c.mode)
      )
    )
