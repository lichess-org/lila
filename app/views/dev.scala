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
              s.form.value match {
                case Some(v: Boolean) =>
                  div(
                    span(cls := "form-check-input")(form3.cmnToggle(s.id, "v", v))
                  )
                case v =>
                  input(
                    name := "v",
                    value := (v match {
                      case None    => ""
                      case Some(x) => x.toString
                      case x       => x.toString
                    })
                  )
              },
              submitButton(cls := "button button-empty", dataIcon := "E")
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
            "Run arbitrary lila commands.",
            br,
            "Only use if you know exactly what you're doing."
          ),
          res map { pre(_) },
          postForm(action := routes.Dev.cliPost)(
            form3.input(form("command"))(autofocus)
          ),
          h2("Command examples:"),
          pre("""uptime
announce 10 minutes Lishogi will restart!
announce cancel
delete lishogi database
change asset version
team disable foobar
team enable foobar
fishnet client create {username}
gdpr erase {username} forever
patron lifetime {username}
patron month {username}
eval-cache drop 9/9/9/2k6/9/3K5/2P6/9/9 b - 1
""")
        )
      )
    }
  }
}
