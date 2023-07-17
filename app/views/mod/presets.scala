package views.html.mod

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import play.api.data.Form
import lila.mod.ModPresets

object presets:

  def apply(group: String, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = s"$group presets",
      moreCss = frag(cssTag("mod.misc"), cssTag("form3"))
    )(
      main(cls := "page-menu")(
        views.html.mod.menu("presets"),
        div(cls := "page-menu__content box box-pad mod-presets")(
          boxTop(
            h1(
              s"$group presets",
              small(
                " / ",
                ModPresets.groups.filter(group !=).map { group =>
                  a(href := routes.Mod.presets(group))(s"$group presets")
                }
              )
            )
          ),
          standardFlash,
          postForm(action := routes.Mod.presetsUpdate(group))(
            form3.group(
              form("v"),
              raw(""),
              help = frag(
                "First line is the permissions needed to use the preset (If a list, separated by commas is given, any user having at least one of these permissions will be able to send it), second is the preset name, next lines are the content. Separate presets with a line of 3 or more dashes: ---."
              ).some
            )(form3.textarea(_)(rows := 20)),
            form3.action(
              submitButton(cls := "button text", dataIcon := licon.Checkmark)("Save")
            )
          )
        )
      )
    )
