package lila

import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.bson._

package object db extends PackageObject with WithPlay with WithDb {

  type WithStringId = { def id: String }

  type QueryBuilder = GenericQueryBuilder[BSONDocument, BSONDocumentReader, BSONDocumentWriter] 
}
