package views.html

import play.api.data.Form

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object dev:

  def settings(settings: List[lila.memo.SettingStore[?]])(using PageContext) =
    val title = "Settings"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    )(
      main(cls := "page-menu")(
        mod.menu("setting"),
        div(id := "settings", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          p("Tread lightly."),
          settings.map { s =>
            postForm(action := routes.Dev.settingsPost(s.id))(
              p(s.text | s.id),
              s.form.value match
                case Some(v: Boolean) => div(span(cls := "form-check-input")(form3.cmnToggle(s.id, "v", v)))
                case v                => input(name := "v", value := v.map(_.toString))
              ,
              submitButton(cls := "button button-empty", dataIcon := licon.Checkmark)
            )
          }
        )
      )
    )

  def cli(form: Form[?], res: Option[String])(using PageContext) =
    val title = "Command Line Interface"
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("mod.misc"), cssTag("form3"))
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("cli"),
        div(id := "dev-cli", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          p(
            "Run arbitrary lila commands.",
            br,
            "Only use if you know exactly what you're doing."
          ),
          res map { pre(_) },
          postForm(action := routes.Dev.cliPost)(
            form3.input(form("command"))(autofocus),
            br,
            form3.submit(frag("Submit"))
          ),
          hr,
          postForm(action := routes.Dev.cliPost)(
            p("Same thing but with a textarea for multiline commands:"),
            form3.textarea(form("command"))(style := "height:8em"),
            br,
            form3.submit(frag("Submit"))
          ),
          h2("Command examples:"),
          pre(cliExamples)
        )
      )

  private val cliExamples = """uptime
announce 10 minutes Lichess will restart!
announce cancel
change asset version
fishnet client create {username}
gdpr erase {username} forever
msg multi {sender} {recipient1,recipient2} {message}
team members add {teamId} {username1,username2,username3}
notify url users {username1,username2,username3} {url} {link title} | {link description}
notify url titled {url} {link title} | {link description}
patron lifetime {username}
patron month {username}
patron remove {username}
tournament feature {id}
tournament unfeature {id}
eval-cache drop standard 8/8/1k6/8/2K5/1P6/8/8 w - - 0 1
disposable test msumain.edu.ph
disposable reload msumain.edu.ph
video sheet
"""
