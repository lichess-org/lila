package lila.db

import java.nio.file.Path

final class Photographer(repo: ImageRepo, prefix: String) {

  import Photographer.uploadMaxMb
  private val uploadMaxBytes        = uploadMaxMb * 1024 * 1024
  private def pictureId(id: String) = s"$prefix:$id:picture"

  def apply(id: String, uploaded: Photographer.Uploaded, createdBy: String): Fu[DbImage] =
    if (uploaded.fileSize > uploadMaxBytes)
      fufail(s"File size must not exceed ${uploadMaxMb}MB.")
    else {

      process(uploaded.ref.path)

      val image = DbImage.make(
        id = pictureId(id),
        name = sanitizeName(uploaded.filename),
        contentType = uploaded.contentType,
        path = uploaded.ref.path,
        size = uploaded.fileSize.toInt,
        createdBy = createdBy
      )

      repo save image inject image
    }

  private def process(path: Path) = {

    import com.sksamuel.scrimage._

    ImmutableImage.loader().fromPath(path).cover(500, 500).output(new nio.JpegWriter(), path)
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
