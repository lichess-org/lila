package lila.title
package ui

import play.api.data.Form
import play.api.libs.json.*

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TitleUi(helpers: Helpers):
  import helpers.{ *, given }

  private def layout(title: String)(using Context) =
    Page(title).css("bits.form3")

  def create(form: Form[?])(using Context) =
    layout("Chess title verification"):
      main(cls := "page-small box box-pad page")(
        postForm(cls := "content_box_content form3", action := routes.TitleVerify.create)(
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
