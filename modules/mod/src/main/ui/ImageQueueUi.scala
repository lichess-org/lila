package lila.mod.ui

import scalalib.paginator.Paginator

import lila.ui.*
import lila.memo.{ PicfitImage, PicfitUrl }

import ScalatagsTemplate.*

final class ImageQueueUi(helpers: Helpers, picfitUrl: PicfitUrl):
  import helpers.*

  def show(flagged: Paginator[PicfitImage]) =
    main(cls := "image-queue infinite-scroll")(
      flagged.currentPageResults.map: image =>
        div(
          a(image.context.map(href := _))(
            img(src := picfitUrl.forAutomod(image.id))
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
