package lila.web
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.common.RawHtml.nl2br

final class DevUi(helpers: Helpers)(modMenu: String => Context ?=> Frag):
  import helpers.*

  def settings(settings: List[(String, List[lila.memo.SettingStore[?]])])(using Context) =
    val title = "Settings"
    Page(title).css("mod.misc"):
      main(cls := "page-menu")(
        modMenu("setting"),
        div(id := "settings", cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(title),
          settings.map: (group, list) =>
            form3.fieldset(group, false.some):
              div: // necessary for styling
                div: // necessary for styling
                  list.map: s =>
                    postForm(action := routes.Dev.settingsPost(s.id))(
                      label(`for` := "v")(s.text | s.id),
                      s.form.value match
                        case Some(v: Boolean) => form3.nativeCheckbox(s.id, "v", v)
                        case Some(v: lila.core.data.Text) => textarea(name := "v")(v.value)
                        case v => input(name := "v", value := v.map(_.toString))
                      ,
                      submitButton(cls := "button button-empty", dataIcon := Icon.Checkmark)
                    )
        )
      )

  def ipTiers(form: Either[String, Form[?]])(using Context) =
    val title = "IP limit tiers"
    Page(title)
      .css("mod.misc")
      .css("bits.form3"):
        main(cls := "page-menu")(
          modMenu("ip-tiers"),
          div(id := "ip-tiers", cls := "page-menu__content box box-pad")(
            h1(cls := "box__top")(title),
            p(
              "Upgrade rate limits for specific IP addresses.",
              br,
              "Only necessary when more than 20 devices connect from the same IP at the same time.",
              br,
              "This requires a service to copy the lila file to the nginx server and reload nginx."
            ),
            p(
              "Format: ",
              br,
              code("{IP} {tier}; # contact info"),
              br,
              nl2br("""
  Tier 1: normal limits (default, up to 30 players)
  Tier 2: higher limits (well enough for schools and hotels)
  Tier 3: much higher limits (only for official bots like maia)
  """)
            ),
            standardFlash,
            postForm(action := routes.Dev.ipTiersPost, cls := "form3")(
              form match
                case Left(err) => p(cls := "error")(err)
                case Right(form) =>
                  val field = form("list")
                  frag(
                    div(cls := "form-group")(
                      form3.textarea(field)(spellcheck := "false"),
                      field.errors.map: err =>
                        p(cls := "error")(nl2br(err.message))
                    ),
                    br,
                    form3.submit(frag("Save and reload nginx"))
                  )
            )
          )
        )

  def cli(form: Form[?], res: Option[String])(using Context) =
    val title = "Command Line Interface"
    Page(title)
      .css("mod.misc")
      .css("bits.form3"):
        main(cls := "page-menu")(
          modMenu("cli"),
          div(id := "dev-cli", cls := "page-menu__content box box-pad")(
            h1(cls := "box__top")(title),
            p(
              "Run arbitrary lila commands.",
              br,
              "Only use if you know exactly what you're doing."
            ),
            res.map { pre(_) },
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
msg multi {sender} {recipient1,recipient2} {message}
team members add {teamId} {username1,username2,username3}
notify url users {username1,username2,username3} {url} {link title} | {link description}
notify url titled {url} {link title} | {link description}
notify url titled-arena {url} {link title} | {link description}
patron lifetime {username}
patron gift-month {username}
patron remove {username}
patron set-months {username} {months}
tournament feature {id}
tournament unfeature {id}
eval-cache drop standard 8/8/1k6/8/2K5/1P6/8/8 w - - 0 1
disposable test msumain.edu.ph
disposable reload msumain.edu.ph
video sheet
puzzle issue {id} {longer-win | ambiguous | ...}
fide player sync
cache clear security.session.info
fide player rip 2026961 2025
"""
