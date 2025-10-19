package lila.memo

import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }
import scalalib.ThreadLocalRandom
import scalalib.paginator.AdapterLike

import lila.common.Bus
import lila.core.id.ImageId
import lila.db.dsl.{ *, given }

final class PicfitApi(
    coll: Coll,
    val url: PicfitUrl,
    ws: StandaloneWSClient,
    config: PicfitConfig,
    cloudflareApi: CloudflareApi
)(using Executor):

  import PicfitApi.{ *, given }
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  val idSep = ':'

  val origin = lila.common.url.origin(config.endpointGet)

  Bus.sub[lila.core.user.UserDelete]: del =>
    for
      ids <- coll.primitive[ImageId]($doc("user" -> del.id), "_id")
      _ <- deleteByIdsAndUser(ids, del.id)
    yield ()

  def uploadFile(
      rel: String,
      uploaded: FilePart,
      userId: UserId,
      meta: Option[form.UploadData] = none
  ): Fu[PicfitImage] =
    val ref: ByteSource = FileIO.fromPath(uploaded.ref.path)
    val source = uploaded.copy[ByteSource](ref = ref, refToBytes = _ => None)
    for
      image <- uploadSource(rel, source, userId, meta)
      dim = meta.fold(Dimensions.default)(_.dim)
      _ = Bus.pub(ImageAutomodRequest(image.id, dim))
    yield image

  def deleteById(id: ImageId): Fu[Option[PicfitImage]] =
    coll
      .findAndRemove($id(id))
      .map:
        _.result[PicfitImage].map: pic =>
          picfitServer.delete(pic)
          pic

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

  def setAutomod(id: ImageId, automod: ImageAutomod): Fu[Option[PicfitImage]] =
    val op = automod.flagged match
      case Some(f) => $set("automod.flagged" -> f)
      case _ => $unset("automod.flagged")
    coll
      .findAndUpdate($id(id), op)
      .map(_.result[PicfitImage])

  def byIds(ids: Iterable[ImageId]): Fu[Seq[PicfitImage]] = coll.byIds(ids)

  def imageFlagAdapter: AdapterLike[PicfitImage] = new:
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

  private def uploadSource(
      rel: String,
      part: SourcePart,
      userId: UserId,
      meta: Option[form.UploadData]
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
              dimensions = meta.map(_.dim),
              context = meta.flatMap(_.context)
            )
            for
              _ <- picfitServer.store(image, part)
              _ <- deleteByRel(image.rel)
              _ <- coll.insert.one(image)
            yield image

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
  private given BSONDocumentHandler[Dimensions] = Macros.handler
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

  object form:

    case class UploadData(dim: Dimensions, context: Option[String] = none)

    import play.api.data.Forms.*
    val pixels = number(min = 20, max = 10_000)
    val dimensions = mapping("width" -> pixels, "height" -> pixels)(Dimensions.apply)(unapply)

    val upload = play.api.data.Form[UploadData]:
      mapping(
        "dim" -> dimensions,
        "context" -> optional(text)
      )(UploadData.apply)(unapply)
