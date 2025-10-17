package lila.memo

import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }
import scalalib.ThreadLocalRandom
import scalalib.paginator.AdapterLike

import lila.common.Bus
import lila.core.id.ImageId
import lila.core.misc.memo.AutomodImageRequest
import lila.db.dsl.{ *, given }

case class PicfitImage(
    @Key("_id") id: ImageId,
    user: UserId,
    // reverse reference like blog:id, streamer:id, coach:id, ...
    // unique: a new image will delete the previous ones with same rel
    rel: String,
    name: String,
    size: Int, // in bytes
    createdAt: Instant,
    meta: Option[ImageMetaData] = none,
    automod: Option[ImageAutomod] = none,
    urls: List[String] = Nil
)

// presence of the ImageAutomod subdoc indicates an image has been scanned, regardless of flagged
case class ImageAutomod(
    flagged: Option[String] = none
)

case class ImageMetaData(
    width: Int,
    height: Int,
    context: Option[String] = none
)

final class PicfitApi(
    coll: Coll,
    ws: StandaloneWSClient,
    config: PicfitConfig,
    cloudflareApi: CloudflareApi
)(using Executor)
    extends lila.core.misc.PicfitApi:

  import PicfitApi.{ *, given }
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  val idSep = ':'

  val origin =
    val pathBegin = config.endpointGet.indexOf('/', 8)
    if pathBegin == -1 then config.endpointGet else config.endpointGet.slice(0, pathBegin)

  Bus.sub[lila.core.user.UserDelete]: del =>
    for
      ids <- coll.primitive[ImageId]($doc("user" -> del.id), "_id")
      _ <- deleteByIdsAndUser(ids, del.id)
    yield ()

  def uploadFile(
      rel: String,
      uploaded: FilePart,
      userId: UserId,
      meta: Option[ImageMetaData] = none
  ): Fu[PicfitImage] =
    val ref: ByteSource = FileIO.fromPath(uploaded.ref.path)
    val source = uploaded.copy[ByteSource](ref = ref, refToBytes = _ => None)
    for
      image <- uploadSource(rel, source, userId, meta)
      (width, height) = meta match
        case Some(m) => (m.width, m.height)
        case _ => (560, 560)
      _ = Bus.pub(AutomodImageRequest(image.id, width, height))
    yield image

  def deleteById(id: ImageId): Funit =
    coll
      .findAndRemove($id(id))
      .flatMap:
        _.result[PicfitImage].so(picfitServer.delete)

  def deleteByIdsAndUser(ids: Seq[ImageId], user: UserId): Funit =
    ids.toList.sequentiallyVoid: id =>
      coll
        .findAndRemove($id(id) ++ $doc("user" -> user))
        .flatMap { _.result[PicfitImage].so(picfitServer.delete) }

  def deleteByRel(rel: String): Funit =
    coll
      .findAndRemove($doc("rel" -> rel))
      .flatMap { _.result[PicfitImage].so(picfitServer.delete) }
      .void

  def setContext(context: String, ids: Seq[ImageId]): Funit =
    coll.update.one($inIds(ids), $set("meta.context" -> context), multi = true).void

  def setAutomod(id: ImageId, automod: ImageAutomod): Funit =
    coll.updateOrUnsetField($id(id), "automod.flagged", automod.flagged).void

  def byIds(ids: Iterable[ImageId]): Fu[Seq[PicfitImage]] = coll.byIds(ids)

  def imageFlagAdapter = new AdapterLike[PicfitImage]:
    lazy val flagCount = countFlagged
    def nbResults: Fu[Int] = flagCount

    def slice(offset: Int, length: Int): Fu[Seq[PicfitImage]] =
      coll
        .aggregateList(length, _.sec): framework =>
          import framework.*
          Match($doc("automod.flagged" -> $exists(true))) -> List(
            Sort(Descending("createdAt")),
            Skip(offset),
            Limit(length)
          )
        .map: docs =>
          for
            doc <- docs
            image <- doc.asOpt[PicfitImage]
          yield image

  def countFlagged = coll.countSel($doc("automod.flagged" -> $exists(true)))

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resizeUrl(
      id: ImageId,
      size: Either[Int, Int] // either the width or the height! the other one will be preserved
  ): String = displayUrl(id, "resize")(
    width = ~size.left.toOption,
    height = ~size.toOption
  )

  // 560x560 containment consumes the minimum 1601 tokens according to the formula here:
  // https://docs.together.ai/docs/vision-overview#pricing
  def automodUrl(id: ImageId, meta: Option[ImageMetaData]) =
    val (width, height) = meta match
      case Some(m) if m.width < m.height => (0, 560)
      case _ => (560, 0)
    displayUrl(id, "resize")(width, height)

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnailUrl(
      id: ImageId,
      width: Int,
      height: Int
  ): String = displayUrl(id, "thumbnail")(width, height)

  def rawUrl(id: ImageId): String =
    val queryString = s"op=noop&path=$id"
    val full = s"${config.endpointGet}/display?${signQueryString(queryString)}"
    discard { recordUrl(id, full) }
    full

  private def uploadSource(
      rel: String,
      part: SourcePart,
      userId: UserId,
      meta: Option[ImageMetaData]
  ): Fu[PicfitImage] =
    if part.fileSize > uploadMaxBytes
    then fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else
      part.contentType
        .collect:
          case "image/webp" => "webp"
          case "image/png" => "png"
          case "image/jpeg" => "jpg"
        .match
          case None => fufail(s"Invalid file type: ${part.contentType | "unknown"}")
          case Some(extension) =>
            val image = PicfitImage(
              id = ImageId(s"$rel$idSep${ThreadLocalRandom.nextString(8)}.$extension"),
              user = userId,
              rel = rel,
              name = part.filename,
              size = part.fileSize.toInt,
              createdAt = nowInstant,
              meta = meta
            )
            for
              _ <- picfitServer.store(image, part)
              _ <- deleteByRel(image.rel)
              _ <- coll.insert.one(image)
            yield image

  private def displayUrl(id: ImageId, operation: "resize" | "thumbnail")(
      width: Int,
      height: Int
  ) =
    // parameters must be given in alphabetical order for the signature to work (!)
    val queryString =
      s"fmt=${if id.value.endsWith(".png") then "png" else "webp"}&h=$height&op=$operation&path=$id&w=$width"
    val full = s"${config.endpointGet}/display?${signQueryString(queryString)}"
    discard { recordUrl(id, full) }
    full

  private def recordUrl(id: ImageId, u: String): Funit =
    // this method is called a lot. an Map[id, Set[url]] or some scaffeine cache would prevent redundant sets
    coll.update.one($id(id), $doc("$addToSet" -> $doc("urls" -> u))).void

  private object signQueryString:
    private val signer = com.roundeights.hasher.Algo.hmac(config.secretKey.value)
    private val cache: com.github.blemale.scaffeine.LoadingCache[String, String] =
      CacheApi.scaffeineNoScheduler
        .expireAfterWrite(10.minutes)
        .build { qs => signer.sha1(qs.replace(":", "%3A")).hex }

    def apply(qs: String) = s"$qs&sig=${cache.get(qs)}"

  private object picfitServer:

    def store(image: PicfitImage, part: SourcePart): Funit =
      ws
        .url(s"${config.endpointPost}/upload")
        .post(Source(part.copy[ByteSource](filename = image.id.value, key = "data") :: Nil))
        .flatMap:
          case res if res.status != 200 => fufail(s"${res.statusText} ${res.body[String].take(200)}")
          case _ =>
            if image.size > 0 then lila.mon.picfit.uploadSize(image.user).record(image.size)
            // else logger.warn(s"Unknown image size: ${image.id} by ${image.user}")
            funit
        .monSuccess(_.picfit.uploadTime(image.user))

    def delete(image: PicfitImage): Funit =
      ws.url(s"${config.endpointPost}/${image.id}")
        .delete()
        .addEffect: res =>
          if res.status / 100 != 2 then
            logger
              .branch("picfit")
              .error(s"deleteFromPicfit ${image.id} ${res.statusText} ${res.body[String].take(200)}")
        .addEffectAnyway:
          cloudflareApi.purge(image.urls)
        .void

object PicfitApi:

  val uploadMaxMb = 6

  type FilePart = MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
  private type ByteSource = Source[ByteString, ?]
  private type SourcePart = MultipartFormData.FilePart[ByteSource]

  private given BSONDocumentHandler[ImageAutomod] = Macros.handler
  private given BSONDocumentHandler[ImageMetaData] = Macros.handler
  private given BSONDocumentHandler[PicfitImage] = Macros.handler

// from playframework/transport/client/play-ws/src/main/scala/play/api/libs/ws/WSBodyWritables.scala
  import play.api.libs.ws.BodyWritable
  private given bodyWritable: BodyWritable[Source[MultipartFormData.Part[ByteSource], ?]] =
    import play.api.libs.ws.SourceBody
    import play.core.formatters.Multipart
    val boundary = Multipart.randomBoundary()
    val contentType = s"multipart/form-data; boundary=$boundary"
    BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)

  def findInMarkdown(md: Markdown): Set[ImageId] =
    // path=ublogBody:mdTLUTfzboGg:wVo9Pqru.jpg
    val regex = """(?i)[\?&]path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})&""".r
    regex
      .findAllMatchIn(md.value)
      .map(_.group(1))
      .map(ImageId(_))
      .toSet

  val uploadForm: play.api.data.Form[ImageMetaData] =
    import play.api.data.Forms.*
    val pixels = number(min = 20, max = 10_000)
    play.api.data.Form(
      mapping(
        "width" -> pixels,
        "height" -> pixels,
        "context" -> optional(text)
      )(ImageMetaData.apply)(unapply)
    )
