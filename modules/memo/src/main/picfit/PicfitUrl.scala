package lila.memo

import lila.core.id.ImageId

final class PicfitUrl(config: PicfitConfig, onNewUrl: (ImageId, Url) => Unit)(using Executor):

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resize(
      id: ImageId,
      size: Either[Int, Int] // either the width or the height! the other one will be preserved
  ): Url =
    display(id, "resize"):
      Dimensions(~size.left.toOption, ~size.toOption)

  def automod(id: ImageId, dim: Option[Dimensions]) =
    display(id, "resize"):
      if dim.exists(_.vertical) then Dimensions.defaultHeight else Dimensions.defaultWidth

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnail(id: ImageId): Dimensions => Url = display(id, "thumbnail")

  def raw(id: ImageId): Url =
    val queryString = s"op=noop&path=$id"
    val url = Url(s"${config.endpointGet}/display?${signQueryString(queryString)}")
    onNewUrl(id, url)
    url

  private def display(id: ImageId, operation: "resize" | "thumbnail")(dim: Dimensions) =
    // parameters must be given in alphabetical order for the signature to work (!)
    val queryString =
      s"fmt=${if id.value.endsWith(".png") then "png" else "webp"}&h=${dim.height}&op=$operation&path=$id&w=${dim.width}"
    val url = Url(s"${config.endpointGet}/display?${signQueryString(queryString)}")
    onNewUrl(id, url)
    url

  private object signQueryString:
    private val signer = com.roundeights.hasher.Algo.hmac(config.secretKey.value)
    private val cache: com.github.blemale.scaffeine.LoadingCache[String, String] =
      CacheApi.scaffeineNoScheduler
        .expireAfterWrite(10.minutes)
        .build { qs => signer.sha1(qs.replace(":", "%3A")).hex }

    def apply(qs: String) = s"$qs&sig=${cache.get(qs)}"
