package views.html

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object dev {

  def settings(settings: List[lidraughts.memo.SettingStore[_]])(implicit ctx: Context) = {
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
              "Run arbitrary lidraughts commands.", br,
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
announce Lidraughts will undergo maintenance in 15 minutes!
change asset version
puzzle disable [standard|frisian] 150
team disable foobar
team enable foobar
draughtsnet client create {username} [analysis|move|commentary|all]
gdpr erase {username} forever
patron [month|lifetime] {username}
eval-cache drop W:W31,32,33,34,50:B1,2,3,4,5,6,7,8,16,17,18,19,20""")
          )
        )
      }
  }
}
