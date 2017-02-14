package lila.db

import com.roundeights.hasher.Implicits._
import java.io.File
import java.nio.file.Files
import org.joda.time.DateTime
import reactivemongo.bson._

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

  def make(id: String, name: String, contentType: Option[String], file: File) = {
    val data = Files.readAllBytes(file.toPath)
    DbImage(
      _id = id,
      data = data,
      hash = data.md5.hex take 8,
      name = name,
      contentType = contentType,
      size = file.length.toInt,
      createdAt = DateTime.now
    )
  }

  implicit val DbImageBSONHandler = Macros.handler[DbImage]
}
