package lila.title
package ui

import play.api.data.Form
import play.api.libs.json.*

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

  def create(form: Form[?])(using Context) =
    layout():
      frag(
        h1(cls := "box__top")("Verify your title"),
        postForm(cls := "form3", action := routes.TitleVerify.create)(
          form3.group(
            form("realName"),
            "Full real name",
            half = true
          )(form3.input(_)(autofocus)),
          form3.group(
            form("title"),
            "Title"
          )(field =>
            form3.select(
              field,
              chess.PlayerTitle.acronyms.map(t => t -> t.value)
            )
          ),
          form3.action(form3.submit("Send"))
        )
      )
