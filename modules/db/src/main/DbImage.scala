package lila.db

import org.joda.time.DateTime

case class DbImage(
  _id: String,
  data: ByteArray,
  name: String,
  createdAt: DateTime)

object DbImage {

}
