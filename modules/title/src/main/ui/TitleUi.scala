package lila.title
package ui

import chess.PlayerTitle
import play.api.data.Form

import lila.core.id.ImageId
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TitleUi(helpers: Helpers)(picfitUrl: lila.core.misc.PicfitUrl):
  import helpers.{ *, given }

  def index(page: Page, intro: Frag) =
    page.css("bits.page"):
      frag(
        intro,
        br,
        br,
        br,
        div(style := "text-align: center;")(
          a(cls := "button button-fat", href := routes.TitleVerify.form)("Verify your title")
        )
      )

  def create(page: Page, form: Form[TitleRequest.FormData])(using Context) =
    page:
      frag(
        h1(cls := "box__top")(page.title),
        postForm(cls := "form3", action := routes.TitleVerify.create)(
          dataForm(form),
          form3.action(form3.submit("Next"))
        )
      )

  def edit(page: Page, form: Form[TitleRequest.FormData], req: TitleRequest)(using Context) =
    page
      .js(esmInitBit("titleRequest")):
        frag(
          h1(cls := "box__top")(page.title),
          standardFlash,
          showStatus(req),
          if req.status.is(_.approved)
          then a(href := routes.TitleVerify.form)("Make a new title request")
          else if req.status.is(_.rejected)
          then emptyFrag
          else showForms(req, form)
        )

  private def showForms(req: TitleRequest, form: Form[TitleRequest.FormData])(using Context) =
    frag(
      div(cls := "title__images")(
        imageByTag(
          req,
          "idDocument",
          name = "Identity document",
          help = div(
            p("ID card, passport or driver's license."),
            p(trans.streamer.maxSize(s"${lila.memo.PicfitApi.uploadMaxMb}MB."))
          )
        ),
        imageByTag(
          req,
          "selfie",
          name = "Your picture",
          help = div(
            p("""A picture of yourself holding up a piece of paper, with the required text:"""),
            pre("""Official Lichess verification
My Lichess account is [your username]
Today's date is [current date]""")
          )
        )
      ),
      postForm(cls := "form3", action := routes.TitleVerify.update(req.id))(
        dataForm(form),
        form3.action(form3.submit("Update and send for review"))
      ),
      postForm(cls := "form3", action := routes.TitleVerify.cancel(req.id))(
        form3.action(
          form3.submit("Cancel request and delete form data", icon = Icon.Trash.some)(
            cls := "button-red button-empty yes-no-confirm"
          )
        )(cls := "title__cancel")
      )
    )

  private def showStatus(req: TitleRequest) =
    import TitleRequest.Status
    div(cls := "title__status-full")(
      statusFlair(req),
      div(cls := "title__status__body")(
        req.status match
          case Status.building => frag("Please upload the required documents to confirm your identity.")
          case Status.pending(_) =>
            frag(
              h2("All set! Your request is pending."),
              "A moderator will review it shortly. You will receive a Lichess message once it is processed."
            )
          case Status.approved =>
            h2("Your ", nbsp, userTitleTag(req.data.title), nbsp, " title has been confirmed!")
          case Status.rejected => h2("Your request has been rejected.")
          case Status.imported => h2("Your request has been archived.")
          case Status.feedback(t) => frag("Moderator feedback:", br, br, strong(t))
      )
    )

  def statusFlair(req: TitleRequest) = iconFlair:
    Flair:
      req.status.name match
        case "approved" => "activity.sparkles"
        case "rejected" => "symbols.cross-mark"
        case "feedback" => "symbols.speech-balloon"
        case "imported" => "objects.books"
        case _ => "objects.hourglass-not-done"

  private def dataForm(form: Form[TitleRequest.FormData])(using Context) =
    frag(
      form3.globalError(form),
      form3.split(
        form3.group(
          form("title"),
          "Title"
        ): field =>
          form3.select(
            field,
            availableTitles.map(t => t -> t.value),
            default = "Select your title".some
          ),
        form3.group(
          form("realName"),
          "Full real name",
          half = true
        )(form3.input(_)(autofocus))
      ),
      form3.split(
        form3.group(
          form("fideId"),
          "Your FIDE ID or profile URL",
          help = frag("If you have one.").some,
          half = true
        )(form3.input(_)),
        form3.group(
          form("federationUrl"),
          "Your national federation profile URL",
          help = frag("If you have one.").some,
          half = true
        )(form3.input(_))
      ),
      form3.split(
        form3.checkbox(
          form("public"),
          frag("Public account"),
          help = frag(
            "Makes your real name and FIDE ID public in your profile. Required for coaching."
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
      )(form3.textarea(_)(rows := 4))
    )

  private def imageByTag(t: TitleRequest, tag: String, name: Frag, help: Frag) =
    val image = t.focusImage(tag).get
    div(cls := "title-image-edit", data("post-url") := routes.TitleVerify.image(t.id, tag))(
      h2(name),
      thumbnail(image, 200)(
        cls := List("drop-target" -> true, "user-image" -> image.isDefined),
        attr("draggable") := "true"
      ),
      help
    )

  object thumbnail:
    def apply(image: Option[ImageId], height: Int): Tag =
      image.fold(fallback): id =>
        img(cls := "title-image", src := url(id, height))
    def fallback = iconTag(Icon.UploadCloud)(cls := "title-image--fallback")
    def url(id: ImageId, height: Int) = picfitUrl.resize(id, Right(height))
    def raw(id: ImageId) = picfitUrl.raw(id)
