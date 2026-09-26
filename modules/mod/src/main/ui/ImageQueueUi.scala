package lila.mod.ui

import scalalib.paginator.Paginator

import lila.ui.*
import lila.memo.{ PicfitImage, PicfitUrl }

import ScalatagsTemplate.{ *, given }

final class ImageQueueUi(helpers: Helpers, picfitUrl: PicfitUrl):
  import helpers.*

  def show(flagged: Paginator[PicfitImage])(using Translate) =
    main(cls := "image-queue infinite-scroll")(
      flagged.currentPageResults.map: image =>
        div(
          a(image.context.map(href := _))(
            img(src := picfitUrl.raw(image.id))
          ),
          div(cls := "image-queue--author")("by ", userIdLink(image.user.some)),
          div(cls := "image-queue--flag")(image.automod.flatMap(_.flagged)),
          div(cls := "image-queue--actions")(
            postForm(action := routes.Mod.imageAccept(image.id, true))(
              button(cls := "button button-empty button-green", dataIcon := Icon.Checkmark)
            ),
            postForm(action := routes.Mod.imageAccept(image.id, false))(
              button(cls := "button button-empty button-red", dataIcon := Icon.Trash)
            )
          )
        ),
      pagerNext(flagged, n => routes.Mod.imageQueue(n).url)
    )
