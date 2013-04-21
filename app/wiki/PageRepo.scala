package lila
package wiki

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import scalaz.effects._

final class PageRepo(
    collection: MongoCollection) extends SalatDAO[Page, String](collection) {

  private val defaultLang = "en"

  def bySlugLang(slug: String, lang: String): IO[Option[Page]] = io {
    findOne(DBObject("slug" -> slug, "lang" -> lang)) orElse
      findOne(DBObject("slug" -> slug, "lang" -> defaultLang))
  }

  def forLang(lang: String): IO[List[Page]] = io {
    find(DBObject("$or" -> DBList(
      "lang" -> lang,
      "lang" -> defaultLang
    ))).sort(DBObject("number" -> 1)).toList
  }

  def saveIO(page: Page): IO[Unit] = io {
    save(page)
  }

  val clear: IO[Unit] = io {
    remove(DBObject())
  }
}
