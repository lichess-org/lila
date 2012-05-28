package lila
package i18n

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.{ MongoCollection, WriteConcern }
import com.mongodb.casbah.Imports._
import scalaz.effects._

class TranslationRepo(
    collection: MongoCollection
  ) extends SalatDAO[Translation, String](collection) {

}
