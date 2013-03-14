package lila.app
package i18n

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

private[i18n] final class TranslationRepo(
    collection: MongoCollection) extends SalatDAO[Translation, String](collection) {

  val nextId: IO[Int] = io {
    collection.find(DBObject(), DBObject("_id" -> true))
      .sort(DBObject("_id" -> -1))
      .limit(1)
      .toList
      .headOption flatMap { _.getAs[Int]("_id") } getOrElse 0
  } map (_ + 1)

  def insertIO(translation: Translation) = io {
    insert(translation)
  }

  def findFrom(id: Int): IO[List[Translation]] = io {
    find("_id" $gte id).sort(DBObject("_id" -> 1)).toList
  }
}
