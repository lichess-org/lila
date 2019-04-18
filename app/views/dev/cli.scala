package views.html.dev

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object cli {

  val title = "Command Line Interface"

  def apply(form: Form[_], res: Option[String])(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = responsiveCssTag("mod.misc")
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
            st.form(action := routes.Dev.cliPost, method := "POST")(
              form3.input(form("command"))(autofocus)
            ),
            h2("Command examples:"),
            pre("""change asset version
puzzle disable 70000
team disable foobar
team enable foobar
fishnet client create {username} analysis
gdpr erase {username} forever
patron lifetime {username}
patron month {username}""")
          )
        )
      }
}
