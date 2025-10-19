package lila.memo

import lila.db.dsl.{ *, given }
import lila.core.id.ImageId

final class PicfitUrl(config: PicfitConfig, coll: Coll)(using Executor):

  val origin =
    val pathBegin = config.endpointGet.indexOf('/', 8)
    if pathBegin == -1 then config.endpointGet else config.endpointGet.slice(0, pathBegin)

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resize(
      id: ImageId,
      size: Either[Int, Int] // either the width or the height! the other one will be preserved
  ): String =
    display(id, "resize"):
      Dimensions(~size.left.toOption, ~size.toOption)

  def automod(id: ImageId, dim: Option[Dimensions]) =
    display(id, "resize"):
      if dim.exists(_.vertical) then Dimensions.defaultHeight else Dimensions.defaultWidth

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnail(id: ImageId): Dimensions => String = display(id, "thumbnail")

  def raw(id: ImageId): String =
    val queryString = s"op=noop&path=$id"
    val full = s"${config.endpointGet}/display?${signQueryString(queryString)}"
    recordUrl(id, full)
    full

  private def display(id: ImageId, operation: "resize" | "thumbnail")(dim: Dimensions) =
    // parameters must be given in alphabetical order for the signature to work (!)
    val queryString =
      s"fmt=${if id.value.endsWith(".png") then "png" else "webp"}&h=${dim.height}&op=$operation&path=$id&w=${dim.width}"
    val full = s"${config.endpointGet}/display?${signQueryString(queryString)}"
    recordUrl(id, full)
    full

  private object recordUrl:
    private val once = scalalib.cache.OnceEvery.hashCode[(ImageId, String)](1.day)
    def apply(id: ImageId, u: String): Unit =
      if once(id, u) then coll.updateUnchecked($id(id), $addToSet("urls" -> u))

  private object signQueryString:
    private val signer = com.roundeights.hasher.Algo.hmac(config.secretKey.value)
    private val cache: com.github.blemale.scaffeine.LoadingCache[String, String] =
      CacheApi.scaffeineNoScheduler
        .expireAfterWrite(10.minutes)
        .build { qs => signer.sha1(qs.replace(":", "%3A")).hex }

    def apply(qs: String) = s"$qs&sig=${cache.get(qs)}"
