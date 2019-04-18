package views.html.dev

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

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
              "Run arbitrary lidraughts commands.", br,
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
puzzle disable [standard|frisian] 150
team disable foobar
team enable foobar
draughtsnet client create {username} [analysis|move|commentary|all]
patron lifetime {username}
patron month {username}
gdpr erase {username} forever""")
          )
        )
      }
}
