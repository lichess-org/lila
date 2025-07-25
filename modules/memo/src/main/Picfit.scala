package lila.memo

import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import com.github.blemale.scaffeine.LoadingCache
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }
import scalalib.ThreadLocalRandom

import lila.core.id.ImageId
import lila.db.dsl.{ *, given }

case class PicfitImage(
    @Key("_id") id: ImageId,
    user: UserId,
    // reverse reference like blog:id, streamer:id, coach:id, ...
    // unique: a new image will delete the previous ones with same rel
    rel: String,
    name: String,
    size: Int, // in bytes
    createdAt: Instant
)

object PicfitImage:

  given BSONDocumentHandler[PicfitImage] = Macros.handler

final class PicfitApi(coll: Coll, val url: PicfitUrl, ws: StandaloneWSClient, config: PicfitConfig)(using
    Executor
):

  import PicfitApi.*
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  val idSep = ':'

  def uploadFile(rel: String, uploaded: FilePart, userId: UserId): Fu[PicfitImage] =
    val ref: ByteSource = FileIO.fromPath(uploaded.ref.path)
    val source = uploaded.copy[ByteSource](ref = ref, refToBytes = _ => None)
    uploadSource(rel, source, userId)

  private def uploadSource(rel: String, part: SourcePart, userId: UserId): Fu[PicfitImage] =
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
              createdAt = nowInstant
            )
            for
              _ <- picfitServer.store(image, part)
              _ <- deleteByRel(image.rel)
              _ <- coll.insert.one(image)
            yield image

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

  object bodyImage:
    val sizePx = Left(800)
    def upload(rel: String, image: FilePart)(using me: Me): Fu[Option[String]] =
      rel.contains(idSep).not.so {
        uploadFile(s"$rel$idSep${scalalib.ThreadLocalRandom.nextString(12)}", image, me)
          .map(pic => url.resize(pic.id, sizePx).some)
      }

  private object picfitServer:

    def store(image: PicfitImage, part: SourcePart): Funit =
      ws
        .url(s"${config.endpointPost}/upload")
        .post(Source(part.copy[ByteSource](filename = image.id.value, key = "data") :: Nil))(using
          WSBodyWritables.bodyWritable
        )
        .flatMap:
          case res if res.status != 200 => fufail(s"${res.statusText} ${res.body[String].take(200)}")
          case _ =>
            if image.size > 0 then lila.mon.picfit.uploadSize(image.user.value).record(image.size)
            // else logger.warn(s"Unknown image size: ${image.id} by ${image.user}")
            funit
        .monSuccess(_.picfit.uploadTime(image.user.value))

    def delete(image: PicfitImage): Funit =
      ws.url(s"${config.endpointPost}/${image.id}")
        .delete()
        .flatMap:
          case res if res.status != 200 =>
            logger
              .branch("picfit")
              .error(s"deleteFromPicfit ${image.id} ${res.statusText} ${res.body[String].take(200)}")
            funit
          case _ => funit

    lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
      for
        ids <- coll.primitive[ImageId]($doc("user" -> del.id), "_id")
        _ <- deleteByIdsAndUser(ids, del.id)
      yield ()

object PicfitApi:

  val uploadMaxMb = 6

  type FilePart = MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
  private type ByteSource = Source[ByteString, ?]
  private type SourcePart = MultipartFormData.FilePart[ByteSource]

// from playframework/transport/client/play-ws/src/main/scala/play/api/libs/ws/WSBodyWritables.scala
  object WSBodyWritables:
    import play.api.libs.ws.BodyWritable
    import play.api.libs.ws.SourceBody
    import play.core.formatters.Multipart
    given bodyWritable: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, ?]], ?]] =
      val boundary = Multipart.randomBoundary()
      val contentType = s"multipart/form-data; boundary=$boundary"
      BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)

  def findInMarkdown(md: Markdown): Set[ImageId] =
    // path=some_username:ublogBody:mdTLUTfzboGg:wVo9Pqru.jpg
    val regex = """(?i)&path=([a-z0-9_-]{2,30}:[a-z]+:\w{12}:\w{8}\.\w{3,4})&""".r
    regex
      .findAllMatchIn(md.value)
      .map(_.group(1))
      .map(ImageId(_))
      .toSet

final class PicfitUrl(config: PicfitConfig)(using Executor) extends lila.core.misc.PicfitUrl:

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resize(
      id: ImageId,
      size: Either[Int, Int] // either the width or the height! the other one will be preserved
  ): String = display(id, "resize")(
    width = ~size.left.toOption,
    height = ~size.toOption
  )

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnail(
      id: ImageId,
      width: Int,
      height: Int
  ): String = display(id, "thumbnail")(width, height)

  def raw(id: ImageId): String =
    val queryString = s"op=noop&path=$id"
    s"${config.endpointGet}/display?${signQueryString(queryString)}"

  private def display(id: ImageId, operation: String)(
      width: Int,
      height: Int
  ) =
    // parameters must be given in alphabetical order for the signature to work (!)
    val queryString =
      s"fmt=${if id.value.endsWith(".png") then "png" else "webp"}&h=$height&op=$operation&path=$id&w=$width"
    s"${config.endpointGet}/display?${signQueryString(queryString)}"

  private object signQueryString:
    private val signer = com.roundeights.hasher.Algo.hmac(config.secretKey.value)
    private val cache: LoadingCache[String, String] =
      CacheApi.scaffeineNoScheduler
        .expireAfterWrite(10.minutes)
        .build { qs => signer.sha1(qs.replace(":", "%3A")).hex }

    def apply(qs: String) = s"$qs&sig=${cache.get(qs)}"
