package lila.db

import java.nio.file.{ Files, Path }
import org.joda.time.DateTime
import reactivemongo.api.bson._

import dsl._

case class DbImage(
    _id: String,
    data: Array[Byte],
    hash: String,
    name: String,
    contentType: Option[String],
    size: Int, // in bytes
    createdAt: DateTime
) {

  def id = _id

  def path = s"$id/$hash/$name"
}

object DbImage {

  def make(id: String, name: String, contentType: Option[String], path: Path, size: Int) = {
    import com.roundeights.hasher.Implicits._
    val data = Files.readAllBytes(path)
    DbImage(
      _id = id,
      data = data,
      hash = data.md5.hex take 8,
      name = name,
      contentType = contentType,
      size = size,
      createdAt = DateTime.now
    )
  }

  implicit val DbImageBSONHandler = Macros.handler[DbImage]
}
