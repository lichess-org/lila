package lila.memo

import akka.stream.scaladsl.{ FileIO, Source, Sink }
import akka.util.ByteString
import java.security.MessageDigest
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }
import reactivemongo.core.errors.DatabaseException
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
)(using Executor, akka.stream.Materializer):

  import PicfitApi.{ *, given }
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  val idSep = ':'

  Bus.sub[lila.core.user.UserDelete]: del =>
    for
      ids <- coll.primitive[ImageId]($doc("user" -> del.id), "_id")
      _ <- deleteByIdsAndUser(ids, del.id)
    yield ()

  def uploadFile(
      rel: String,
      uploaded: FilePart,
      userId: UserId,
      meta: Option[form.UploadData] = none,
      requestAutomod: Boolean = true
  ): Fu[PicfitImage] =
    for
      hash <- FileIO
        .fromPath(uploaded.ref.path)
        .runWith(hashSink)
        .map: bytes =>
          java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).take(12)
      ref = FileIO.fromPath(uploaded.ref.path)
      source = uploaded.copy[ByteSource](ref = ref, refToBytes = _ => None)
      uploaded <- uploadSource(rel, hash, source, userId, meta)
      dim = meta.fold(Dimensions.default)(_.dim)
      _ = if requestAutomod && uploaded.fresh then Bus.pub(ImageAutomodRequest(uploaded.image.id, dim))
    yield uploaded.image

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
    if !rel.has(':') then fuccess(s"PicFitApi.deleteByRel not gonna delete $rel")
    else
      coll
        .findAndRemove($doc("rel" -> rel))
        .flatMap { _.result[PicfitImage].so(picfitServer.delete) }
        .void

  def setContext(context: String, ids: Seq[ImageId]): Funit =
    coll.update.one($inIds(ids), $set("context" -> context), multi = true).void

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

  private val hashSink: Sink[ByteString, Future[Array[Byte]]] =
    Sink
      .fold(MessageDigest.getInstance("SHA-256")): (hasher, bytes: ByteString) =>
        hasher.update(bytes.asByteBuffer)
        hasher
      .mapMaterializedValue(_.map(_.digest()))

  private def uploadSource(
      rel: String,
      hash: String,
      part: SourcePart,
      userId: UserId,
      meta: Option[form.UploadData]
  ): Fu[ImageFresh] =
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
              id = ImageId(s"$rel$idSep$hash.$extension"),
              user = userId,
              rel = rel,
              name = part.filename,
              size = part.fileSize.toInt,
              createdAt = nowInstant,
              dimensions = meta.map(_.dim),
              context = meta.flatMap(_.context)
            )
            coll.insert
              .one(image)
              .flatMap: _ =>
                picfitServer
                  .store(image, part).inject(ImageFresh(image, true))
                  .recoverWith: e =>
                      coll.delete.one($id(image.id))
                      fufail(e)
              .recoverWith:
                case e: DatabaseException if e.code.contains(11000) =>
                  fuccess(ImageFresh(image, false)) // it's a dup

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
  case class ImageFresh(image: PicfitImage, fresh: Boolean)

  private[memo] final class OnNewUrl(coll: Coll)(using Executor):

    private val once = scalalib.cache.OnceEvery.hashCode[(ImageId, Url)](1.day)

    def apply(id: ImageId, u: Url): Unit =
      if once(id, u) then coll.updateUnchecked($id(id), $addToSet("urls" -> u))

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
    // path=ublogBody:mdTLU-fz_oGg.jpg
    val regex = """(?i)[\?&]path=([a-z]\w+:[-_a-z0-9]{12}\.\w{3,4})&""".r
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
