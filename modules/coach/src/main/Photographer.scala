package lila.coach

import java.io.File

import lila.db.DbImage
import lila.db.dsl._

private final class Photographer(coll: Coll) {

  private val uploadedMaxMb = 3
  private val uploadedMaxBytes = uploadedMaxMb * 1024 * 1024
  private def pictureId(id: Coach.Id) = s"coach:${id.value}:picture"

  def apply(coachId: Coach.Id, uploaded: Photographer.Uploaded): Fu[DbImage] =
    if (uploaded.ref.file.length > uploadedMaxBytes)
      fufail(s"File size must not exceed ${uploadedMaxMb}MB.")
    else {

      process(uploaded.ref.file)

      val image = DbImage.make(
        id = pictureId(coachId),
        name = uploaded.filename,
        contentType = uploaded.contentType,
        file = uploaded.ref.file)

      coll.update($id(image.id), image, upsert = true) inject image
    }

  private def process(file: File) = {

    import com.sksamuel.scrimage._

    Image.fromFile(file).cover(500, 500).output(file)
  }
}

private object Photographer {

  type Uploaded = play.api.mvc.MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]
}
