package views.html.mod

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.memo.SettingStore
import play.api.data.Form
import lila.mod.ModPresets

object presets {

  def apply(group: String, setting: SettingStore[ModPresets], form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"$group presets",
      moreCss = frag(cssTag("mod.misc"), cssTag("form3"))
    )(
      main(cls := "page-menu")(
        views.html.mod.menu("presets"),
        div(cls := "page-menu__content box box-pad mod-presets")(
          h1(
            s"$group presets",
            small(
              " / ",
              ModPresets.groups.filter(group !=).map { group =>
                a(href := routes.Mod.presets(group))(s"$group presets")
              }
            )
          ),
          standardFlash(),
          postForm(action := routes.Mod.presetsUpdate(group))(
            form3.group(
              form("v"),
              raw(""),
              help = frag(
                "First line is the preset name, next lines are the content. Separate presets with a line of 3 or more dashes: ---."
              ).some
            )(form3.textarea(_)(rows := 20)),
            form3.action(
              submitButton(cls := "button text", dataIcon := "")("Save")
            )
          )
        )
      )
    )
}
