package lila.mod.ui

import scalalib.paginator.Paginator

import lila.ui.*
import lila.memo.{ PicfitImage, PicfitApi }

import ScalatagsTemplate.*

final class ImageQueueUi(helpers: Helpers, picfitApi: PicfitApi):
  import helpers.*

  def show(flagged: Paginator[PicfitImage]) =
    main(cls := "image-queue infinite-scroll")(
      flagged.currentPageResults.map: image =>
        div(
          a(image.meta.flatMap(_.context.map(href := _)))(
            img(src := picfitApi.rawUrl(image.id))
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
