package lila.memo

import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import com.github.blemale.scaffeine.LoadingCache
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }
import ornicar.scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }
import lila.common.IpAddress

case class PicfitImage(
    _id: PicfitImage.Id,
    user: UserId,
    // reverse reference like blog:id, streamer:id, coach:id, ...
    // unique: a new image will delete the previous ones with same rel
    rel: String,
    name: String,
    size: Int, // in bytes
    createdAt: Instant
):
  inline def id = _id

object PicfitImage:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  given BSONDocumentHandler[PicfitImage] = Macros.handler

final class PicfitApi(coll: Coll, val url: PicfitUrl, ws: StandaloneWSClient, config: PicfitConfig)(using
    Executor
):

  import PicfitApi.*
  import PicfitImage.*
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  def uploadFile(rel: String, uploaded: FilePart, userId: UserId): Fu[PicfitImage] =
    val ref: ByteSource = FileIO.fromPath(uploaded.ref.path)
    val source          = uploaded.copy[ByteSource](ref = ref, refToBytes = _ => None)
    uploadSource(rel, source, userId)

  def uploadSource(rel: String, part: SourcePart, userId: UserId): Fu[PicfitImage] =
    if part.fileSize > uploadMaxBytes then fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else
      part.contentType
        .collect:
          case "image/png"  => "png"
          case "image/jpeg" => "jpg"
        .match
          case None => fufail(s"Invalid file type: ${part.contentType | "unknown"}")
          case Some(extension) =>
            val image = PicfitImage(
              _id = PicfitImage.Id(s"$userId:$rel:${ThreadLocalRandom nextString 8}.$extension"),
              user = userId,
              rel = rel,
              name = part.filename,
              size = part.fileSize.toInt,
              createdAt = nowInstant
            )
            picfitServer.store(image, part) >>
              deleteByRel(image.rel) >>
              coll.insert.one(image) inject image

  def deleteByRel(rel: String): Funit =
    coll
      .findAndRemove($doc("rel" -> rel))
      .flatMap { _.result[PicfitImage] so picfitServer.delete }
      .void

  object bodyImage:
    private val sizePx = Left(720)
    private val RateLimitPerIp = RateLimit.composite[lila.common.IpAddress](key = "image.body.upload.ip")(
      ("fast", 10, 2.minutes),
      ("slow", 60, 1.day)
    )
    def upload(rel: String, image: FilePart, me: UserId, ip: IpAddress): Fu[Option[String]] =
      RateLimitPerIp(ip, fuccess(none)):
        uploadFile(s"$rel:${ornicar.scalalib.ThreadLocalRandom.nextString(12)}", image, me)
          .map(pic => url.resize(pic.id, sizePx).some)

  private object picfitServer:

    def store(image: PicfitImage, part: SourcePart): Funit =
      ws
        .url(s"${config.endpointPost}/upload")
        .post(Source(part.copy[ByteSource](filename = image.id.value, key = "data") :: List()))(using
          WSBodyWritables.bodyWritable
        )
        .flatMap:
          case res if res.status != 200 => fufail(s"${res.statusText} ${res.body[String] take 200}")
          case _ =>
            lila.mon.picfit.uploadSize(image.user.value).record(image.size)
            funit
        .monSuccess(_.picfit.uploadTime(image.user.value))

    def delete(image: PicfitImage): Funit =
      ws.url(s"${config.endpointPost}/${image.id}").delete().flatMap {
        case res if res.status != 200 =>
          logger
            .branch("picfit")
            .error(s"deleteFromPicfit ${image.id} ${res.statusText} ${res.body[String] take 200}")
          funit
        case _ => funit
      }

object PicfitApi:

  val uploadMaxMb = 4

  type FilePart           = MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
  private type ByteSource = Source[ByteString, ?]
  private type SourcePart = MultipartFormData.FilePart[ByteSource]

// from playframework/transport/client/play-ws/src/main/scala/play/api/libs/ws/WSBodyWritables.scala
  object WSBodyWritables:
    import play.api.libs.ws.BodyWritable
    import play.api.libs.ws.SourceBody
    import play.core.formatters.Multipart
    given bodyWritable: BodyWritable[Source[MultipartFormData.Part[Source[ByteString, ?]], ?]] =
      val boundary    = Multipart.randomBoundary()
      val contentType = s"multipart/form-data; boundary=$boundary"
      BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)

final class PicfitUrl(config: PicfitConfig):

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resize(
      id: PicfitImage.Id,
      size: Either[Int, Int] // either the width or the height! the other one will be preserved
  ) = display(id, "resize")(
    width = ~size.left.toOption,
    height = ~size.toOption
  )

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnail(
      id: PicfitImage.Id,
      width: Int,
      height: Int
  ) = display(id, "thumbnail")(width, height)

  private def display(id: PicfitImage.Id, operation: String)(
      width: Int,
      height: Int
  ) =
    // parameters must be given in alphabetical order for the signature to work (!)
    val queryString = s"h=$height&op=$operation&path=$id&w=$width"
    s"${config.endpointGet}/display?${signQueryString(queryString)}"

  private object signQueryString:
    private val signer = com.roundeights.hasher.Algo hmac config.secretKey.value
    private val cache: LoadingCache[String, String] =
      CacheApi.scaffeineNoScheduler
        .expireAfterWrite(10 minutes)
        .build { qs => signer.sha1(qs.replace(":", "%3A")).hex }

    def apply(qs: String) = s"$qs&sig=${cache get qs}"
