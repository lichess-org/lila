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
          a(image.context.map(href := _))(
            image.urls.headOption.map(url => img(src := url))
          ),
          image.automod.flatMap(_.flagged),
          span(
            postForm(action := routes.Mod.passImage(image.id).url)(
              button(cls := "button button-empty")("Pass")
            ),
            postForm(action := routes.Mod.purgeImage(image.id).url)(
              button(cls := "button button-empty button-red")("Purge")
            )
          )
        ),
      pagerNext(flagged, n => routes.Mod.imageQueue(n).url)
    )
