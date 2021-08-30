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
    scope: String, // like blog, streamer, coach, ...
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

  def upload(scope: String, uploaded: Uploaded, userId: String): Fu[PicfitImage] =
    if (uploaded.fileSize > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {
      import WSBodyWritables._
      val image = PicfitImage(
        _id = PicfitImage.Id(lila.common.ThreadLocalRandom nextString 10),
        user = userId,
        scope = scope,
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        size = uploaded.fileSize.toInt,
        createdAt = DateTime.now
      )
      type Part = MultipartFormData.FilePart[Source[ByteString, _]]
      val part: Part = MultipartFormData.FilePart(
        key = "data",
        filename = image.id.value,
        contentType = uploaded.contentType,
        ref = FileIO.fromPath(uploaded.ref.path),
        fileSize = uploaded.fileSize
      )
      val source: Source[Part, _] = Source(part :: List())
      ws.url(s"$endpoint/upload").post(source).flatMap {
        case res if res.status != 200 => fufail(res.statusText)
        case _                        => funit
      } >>
        coll.insert.one(image) inject image
    }

  private def sanitizeName(name: String) = {
    // the char `^` breaks play, even URL encoded
    java.net.URLEncoder.encode(name, "UTF-8").replaceIf('%', "")
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
