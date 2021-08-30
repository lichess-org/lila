package lila.memo

import java.nio.file.Path
import org.joda.time.DateTime
import reactivemongo.api.bson.Macros
import scala.concurrent.ExecutionContext
import play.api.libs.ws.StandaloneWSClient

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
      val image = PicfitImage(
        _id = PicfitImage.Id(lila.common.ThreadLocalRandom nextString 8),
        user = userId,
        scope = scope,
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        size = uploaded.fileSize.toInt,
        createdAt = DateTime.now
      )
      // ws.url(s"$endpoint/upload")
      //   .post(
      //     Source(
      //       FilePart(
      //         "File",
      //         fileName,
      //         Option("application/pdf"),
      //         FileIO.fromPath(Paths.get(pathToFile))
      //       ) :: List()
      //     )
      //   )
      //   .post(body(source)) flatMap {
      //   case res if res.status != 200 =>
      //     fufail(res.statusText)
      //   case res => funit
      // } >>
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
}
