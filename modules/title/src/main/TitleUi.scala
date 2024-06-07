package lila.title
package ui

import play.api.data.Form
import play.api.libs.json.*
import chess.PlayerTitle

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.id.ImageId

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

  def create(form: Form[TitleRequest.FormData])(using Context) =
    layout():
      frag(
        h1(cls := "box__top")("Verify your title"),
        dataForm(form, none)
      )

  private def dataForm(form: Form[TitleRequest.FormData], req: Option[TitleRequest])(using Context) =
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

  // private def image(t: RelayTour)(using ctx: Context) =
  //   div(cls := "relay-image-edit", data("post-url") := routes.RelayTour.image(t.id))(
  //     ui.thumbnail(t.image, _.Size.Small)(
  //       cls               := List("drop-target" -> true, "user-image" -> t.image.isDefined),
  //       attr("draggable") := "true"
  //     ),
  //     div(
  //       p("Upload a beautiful image to represent your tournament."),
  //       p("The image must be twice as wide as it is tall. Recommended resolution: 1000x500."),
  //       p(
  //         "A picture of the city where the tournament takes place is a good idea, but feel free to design something different."
  //       ),
  //       p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB.")),
  //       form3.file.selectImage()
  //     )
  //   )
