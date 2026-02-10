package lila.memo

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.{ BSONDocumentHandler, BSONDocumentReader, Macros }
import reactivemongo.core.errors.DatabaseException
import scala.util.matching.Regex.quote
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

  Bus.sub[lila.core.user.UserDelete](del => deleteByUser(del.id))

  def uploadFile(
      file: FilePart,
      userId: UserId,
      uniqueRef: Option[String] = none,
      meta: Option[form.UploadData] = none,
      requestAutomod: Boolean = true
  ): Fu[PicfitImage] =
    val hash = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(file.ref.sha256).take(12)
    uploadSource(file, hash, userId, uniqueRef, meta = meta).map: uploaded =>
      if requestAutomod && uploaded.fresh then
        Bus.pub(ImageAutomodRequest(uploaded.image.id, meta.fold(Dimensions.default)(_.dim)))
      uploaded.image

  def addRef(markdown: Markdown, ref: String, context: Option[String] = none): Funit =
    addRef(imageIds(markdown), ref, context)

  def pullRef(ref: String): Funit =
    if !ref.has(':') then fufail(s"PicfitApi.pullRef cant pull '$ref'")
    else
      for
        ids <- coll.primitive[ImageId]($doc("refs" -> ref), "_id")
        _ <- ids.nonEmpty.so:
          coll.update(ordered = false).one($inIds(ids), $pull("refs" -> ref), multi = true).void
        _ <- ids.nonEmpty.so:
          coll.delete.one($inIds(ids) ++ $doc("refs" -> $doc("$size" -> 0))).void
      yield ()

  def deleteById(id: ImageId): Fu[Option[PicfitImage]] =
    coll
      .findAndRemove($id(id))
      .map:
        _.result[PicfitImage].map: pic =>
          picfitServer.delete(pic)
          pic

  def setAutomod(id: ImageId, automod: ImageAutomod): Fu[Option[PicfitImage]] =
    val op = automod.flagged match
      case Some(f) => $set("automod.flagged" -> f)
      case _ => $unset("automod.flagged")
    coll
      .findAndUpdate($id(id), op)
      .map(_.result[PicfitImage])

  def imageIds(markdown: Markdown): Seq[ImageId] =
    imageIdRe.findAllMatchIn(markdown.value).map(m => ImageId(m.group(1))).toSeq

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
      file: FilePart,
      hash: String,
      userId: UserId,
      uniqueRef: Option[String],
      meta: Option[form.UploadData]
  ): Fu[ImageFresh] =
    file.contentType
      .collect:
        case "image/webp" => "webp"
        case "image/png" => "png"
        case "image/jpeg" => "jpg"
      .match
        case None => fufail(s"Invalid file type: ${file.contentType | "unknown"}")
        case Some(extension) =>
          val image = PicfitImage(
            id = ImageId(s"$hash.$extension"),
            user = userId,
            name = file.filename,
            size = file.fileSize.toInt,
            createdAt = nowInstant,
            refs = uniqueRef.toList,
            dimensions = meta.map(_.dim),
            context = meta.flatMap(_.context)
          )
          for
            _ <- uniqueRef.so(pullRef)
            imageFresh <- updateColl(image)
            _ <- imageFresh.fresh so picfitServer.store(image, file)
          yield imageFresh

  private def updateColl(image: PicfitImage): Fu[ImageFresh] =
    coll.insert
      .one(image)
      .inject(ImageFresh(image, true))
      .recoverWith:
        case e: DatabaseException if e.code.contains(11000) =>
          if image.refs.nonEmpty then
            for _ <- coll.update.one($id(image.id), $addToSet("refs" -> $doc("$each" -> image.refs)))
            yield ImageFresh(image, false)
          else fuccess(ImageFresh(image, false))

  private def addRef(ids: Seq[ImageId], ref: String, context: Option[String]): Funit =
    if ids.isEmpty then funit
    else
      coll
        .update(ordered = false)
        .one(
          $inIds(ids),
          $addToSet("refs" -> ref) ++ context.fold($doc())(ctx => $set("context" -> ctx)),
          multi = true
        )
        .void

  def deleteByUser(userId: UserId): Funit =
    for
      ids <- coll.primitive[ImageId]($doc("user" -> userId), "_id")
      _ <- ids.toList.sequentiallyVoid(deleteById)
    yield ()

  private val imageIdRe =
    raw"""(?i)!\[(?:[^\n\]]*+)\]\(${quote(
        lila.common.url.origin(config.endpointGet).value
      )}[^)\s]+[?&]path=((?:[a-z]\w+:)?[-_a-z0-9]{12}\.\w{3,4})[^)]*\)""".r

  private object picfitServer:

    def store(image: PicfitImage, file: FilePart): Funit =
      val sourcePart = MultipartFormData.FilePart[ByteSource](
        key = "data",
        filename = image.id.value,
        contentType = file.contentType,
        ref = file.ref.source,
        fileSize = file.fileSize,
        dispositionType = file.dispositionType,
        refToBytes = _ => None
      )

      ws.url(s"${config.endpointPost}/upload")
        .post(Source.single(sourcePart))
        .flatMap {
          case res if res.status != 200 =>
            fufail(s"${res.statusText} ${res.body[String].take(200)}")
          case _ =>
            if image.size > 0 then lila.mon.picfit.uploadSize(image.user).record(image.size)
            funit
        }
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
  val idSep = ':'

  type FilePart = MultipartFormData.FilePart[HashedSource]
  type ByteSource = Source[ByteString, ?]

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
