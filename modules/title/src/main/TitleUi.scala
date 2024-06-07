package lila.title
package ui

import play.api.data.Form
import play.api.libs.json.*
import chess.PlayerTitle

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TitleUi(helpers: Helpers):
  import helpers.{ *, given }

  private def layout(title: String = "Title verification")(using Context) =
    Page(title)
      .css("bits.form3")
      .wrap: body =>
        main(cls := "page-small box box-pad page")(body)

  def index(title: String, intro: Frag)(using Context) =
    layout(title).css("bits.page"):
      frag(
        intro,
        br,
        br,
        br,
        div(style := "text-align: center;")(
          a(cls := "button button-fat", href := routes.TitleVerify.form)("Verify your title")
        )
      )

  private val availableTitles = PlayerTitle.acronyms.filter: t =>
    t != PlayerTitle.LM && t != PlayerTitle.BOT

  def create(form: Form[?])(using Context) =
    layout():
      frag(
        h1(cls := "box__top")("Verify your title"),
        postForm(cls := "form3", action := routes.TitleVerify.create)(
          form3.globalError(form).pp(form),
          form3.group(
            form("realName"),
            "Full real name",
            half = true
          )(form3.input(_)(autofocus)),
          form3.group(
            form("title"),
            "Title"
          ): field =>
            form3.select(
              field,
              availableTitles.map(t => t -> t.value),
              default = "Select your title".some
            ),
          form3.split(
            form3.group(
              form("fideId"),
              "Your FIDE ID or profile URL",
              help = frag("If you have one.").some,
              half = true
            )(form3.input(_)),
            form3.group(
              form("nationalFederationUrl"),
              "Your national federation profile URL",
              help = frag("If you have one.").some,
              half = true
            )(form3.input(_))
          ),
          form3.split(
            form3.checkbox(
              form("public").copy(value = "true".some),
              frag("Publish my title"),
              help = frag(
                "Recommended. Your title is visible on your profile page. Your real name is NOT published."
              ).some,
              half = true
            ),
            form3.checkbox(
              form("coach"),
              frag("Create a coach profile"),
              help = frag(
                "Offer your services as a coach, and appear in the ",
                a(href := routes.Coach.all())("coach directory"),
                "."
              ).some,
              half = true
            )
          ),
          form3.group(
            form("comment"),
            "Comment",
            help = frag("Optional additional information for the moderators.").some,
            half = true
          )(form3.textarea(_)(rows := 8)),
          form3.action(form3.submit("Send"))
        )
      )
