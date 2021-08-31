package lila.memo

import akka.stream.scaladsl.{ FileIO, Source }
import akka.util.ByteString
import org.joda.time.DateTime
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.MultipartFormData
import reactivemongo.api.bson.Macros
import scala.concurrent.ExecutionContext

import lila.db.dsl._

case class PicfitImage(
    _id: PicfitImage.Id,
    user: String,
    // reverse reference like blog:id, streamer:id, coach:id, ...
    // unique: a new image will delete the previous ones with same rel
    rel: String,
    name: String,
    contentType: Option[String],
    size: Int, // in bytes
    createdAt: DateTime
) {

  def id = _id
}

object PicfitImage {

  case class Id(value: String) extends AnyVal with StringValue

  implicit val imageIdBSONHandler = stringAnyValHandler[PicfitImage.Id](_.value, PicfitImage.Id.apply)
  implicit val imageBSONHandler   = Macros.handler[PicfitImage]
}

final class PicfitApi(coll: Coll, ws: StandaloneWSClient, endpoint: String)(implicit ec: ExecutionContext) {

  import PicfitApi._
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024

  def upload(rel: String, uploaded: Uploaded, userId: String): Fu[PicfitImage] =
    if (uploaded.fileSize > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {
      val image = PicfitImage(
        _id = PicfitImage.Id(lila.common.ThreadLocalRandom nextString 10),
        user = userId,
        rel = rel,
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        size = uploaded.fileSize.toInt,
        createdAt = DateTime.now
      )
      picfitServer.store(image, uploaded) >>
        deletePrevious(image) >>
        coll.insert.one(image) inject image
    }

  private def deletePrevious(image: PicfitImage): Funit =
    coll
      .findAndRemove($doc("rel" -> image.rel, "_id" $ne image.id))
      .flatMap { _.result[PicfitImage] ?? picfitServer.delete }
      .void

  private def sanitizeName(name: String) = {
    // the char `^` breaks play, even URL encoded
    java.net.URLEncoder.encode(name, "UTF-8").replaceIf('%', "")
  }

  private object picfitServer {

    def store(image: PicfitImage, from: Uploaded): Funit = {
      type Part = MultipartFormData.FilePart[Source[ByteString, _]]
      import WSBodyWritables._
      val part: Part = MultipartFormData.FilePart(
        key = "data",
        filename = image.id.value,
        contentType = from.contentType,
        ref = FileIO.fromPath(from.ref.path),
        fileSize = from.fileSize
      )
      val source: Source[Part, _] = Source(part :: List())
      ws.url(s"$endpoint/upload")
        .post(source)
        .flatMap {
          case res if res.status != 200 => fufail(res.statusText)
          case _ =>
            lila.mon.picfit.uploadSize(image.user).record(image.size)
            funit
        }
        .monSuccess(_.picfit.uploadTime(image.user))
    }

    def delete(image: PicfitImage): Funit =
      ws.url(s"$endpoint/${image.id}").delete().flatMap {
        case res if res.status != 200 =>
          logger
            .branch("picfit")
            .error(s"deleteFromPicfit ${image.id} ${res.statusText} ${res.body take 200}")
          funit
        case _ => funit
      }
  }
}

object PicfitApi {

  val uploadMaxMb = 4

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]

// from playframework/transport/client/play-ws/src/main/scala/play/api/libs/ws/WSBodyWritables.scala
  object WSBodyWritables {
    import play.api.libs.ws.BodyWritable
    import play.api.libs.ws.SourceBody
    import play.core.formatters.Multipart
    implicit val bodyWritableOf_Multipart
        : BodyWritable[Source[MultipartFormData.Part[Source[ByteString, _]], _]] = {
      val boundary    = Multipart.randomBoundary()
      val contentType = s"multipart/form-data; boundary=$boundary"
      BodyWritable(b => SourceBody(Multipart.transform(b, boundary)), contentType)
    }
  }
}

final class PicfitUrl(endpoint: String, id: PicfitImage.Id) {

  // This operation will able you to resize the image to the specified width and height.
  // Preserves the aspect ratio
  def resize(
      size: Either[Int, Int], // either the width or the height! the other one will be preserved
      upscale: Boolean = true
  ) = display(id, "resize")(
    width = ~size.left.toOption,
    height = ~size.toOption,
    upscale = upscale
  )

  // Thumbnail scales the image up or down using the specified resample filter,
  // crops it to the specified width and height and returns the transformed image.
  // Preserves the aspect ratio
  def thumbnail(
      width: Int,
      height: Int,
      upscale: Boolean = true
  ) = display(id, "thumbnail")(width, height, upscale)

  private def display(id: PicfitImage.Id, operation: String)(
      width: Int,
      height: Int,
      upscale: Boolean
  ) =
    s"$endpoint/display?path=$id&op=$operation&w=$width&h=$height&upscale=${upscale ?? 1}"
}
