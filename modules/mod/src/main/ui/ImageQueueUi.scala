package lila.mod.ui

import scalalib.paginator.Paginator

import lila.ui.*
import lila.memo.PicfitImage

import ScalatagsTemplate.*

final class ImageQueueUi(helpers: Helpers):
  import helpers.*

  def show(flagged: Paginator[PicfitImage]) =
    main(cls := "image-queue infinite-scroll")(
      flagged.currentPageResults.map: image =>
        div(
          a(image.context.map(ctx => href := ctx))(
            image.urls.nonEmpty.option(img(src := image.urls(0)))
          ),
          image.automod.flatMap(_.flagged),
          span(
            form(method := "POST", action := routes.Mod.passImage(image.id).url)(
              button(cls := "button button-empty")("Pass")
            ),
            form(method := "POST", action := routes.Mod.purgeImage(image.id).url)(
              button(cls := "button button-empty button-red")("Purge")
            )
          )
        ),
      pagerNext(flagged, n => routes.Mod.imageQueue(n).url)
    )
