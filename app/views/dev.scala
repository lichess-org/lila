package views.html

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object dev {

  def settings(settings: List[lila.memo.SettingStore[_]])(implicit ctx: Context) = {
    val title = "Settings"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    )(
        main(cls := "page-menu")(
          mod.menu("setting"),
          div(id := "settings", cls := "page-menu__content box box-pad")(
            h1(title),
            p("Tread lightly."),
            settings.map { s =>
              postForm(action := routes.Dev.settingsPost(s.id))(
                p(s.text | s.id),
                input(name := "v", value := (s.form.value match {
                  case None => ""
                  case Some(x) => x.toString
                  case x => x.toString
                })),
                submitButton(cls := "button", dataIcon := "E")
              )
            }
          )
        )
      )
  }

  def cli(form: Form[_], res: Option[String])(implicit ctx: Context) = {
    val title = "Command Line Interface"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    ) {
        main(cls := "page-menu")(
          views.html.mod.menu("cli"),
          div(id := "dev-cli", cls := "page-menu__content box box-pad")(
            h1(title),
            p(
              "Run arbitrary lila commands.", br,
              "Only use if you know exactly what you're doing."
            ),
            res.map { r =>
              h2("Result:")
              pre(r)
            },
            postForm(action := routes.Dev.cliPost)(
              form3.input(form("command"))(autofocus)
            ),
            h2("Command examples:"),
            pre("""uptime
announce 10 minutes Lichess will restart!
announce cancel
change asset version
puzzle disable 70000
team disable foobar
team enable foobar
fishnet client create {username} analysis
gdpr erase {username} forever
patron lifetime {username}
patron month {username}
eval-cache drop 8/8/1k6/8/2K5/1P6/8/8 w - - 0 1""")
          )
        )
      }
  }
}
