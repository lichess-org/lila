package lila.db

import java.nio.file.Path

import dsl._

final class Photographer(coll: Coll, prefix: String) {

  import Photographer.uploadMaxMb
  private val uploadMaxBytes = uploadMaxMb * 1024 * 1024
  private def pictureId(id: String) = s"$prefix:$id:picture"

  def apply(id: String, uploaded: Photographer.Uploaded): Fu[DbImage] =
    if (uploaded.fileSize > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {

      process(uploaded.ref.path)

      val image = DbImage.make(
        id = pictureId(id),
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        path = uploaded.ref.path,
        size = uploaded.fileSize.toInt
      )

      coll.update.one($id(image.id), image, upsert = true) inject image
    }

  private def process(path: Path) = {

    import com.sksamuel.scrimage._

    Image.fromPath(path).cover(500, 500).output(path)
  }

  private def sanitizeName(name: String) = {
    // the char `^` breaks play, even URL encoded
    java.net.URLEncoder.encode(name, "UTF-8").replaceIf('%', "")
  }
}

object Photographer {

  val uploadMaxMb = 3

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
}
