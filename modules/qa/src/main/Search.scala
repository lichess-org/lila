package lila.qa

import reactivemongo.bson._
import reactivemongo.core.commands._

import lila.db.dsl._

final class Search(collection: Coll) {

  private implicit val commentBSONHandler = Macros.handler[Comment]
  private implicit val voteBSONHandler = Macros.handler[Vote]
  private[qa] implicit val questionBSONHandler = Macros.handler[Question]

  private type Result = List[BSONDocument]

  private case class Search(
      collectionName: String,
      search: String,
      filter: Option[BSONDocument] = None) extends Command[Result] {

    override def makeDocuments = BSONDocument(
      "text" -> collectionName,
      "search" -> search,
      "filter" -> filter)

    val ResultMaker = new BSONCommandResultMaker[Result] {
      /**
       * Deserializes the given response into an instance of Result.
       */
      def apply(document: BSONDocument): Either[CommandError, Result] =
        CommandError.checkOk(document, Some("search")) toLeft {
          document.getAs[List[BSONDocument]]("results") getOrElse Nil
        }
    }
  }

  def apply(q: String): Fu[List[Question]] =
    collection.find(BSONDocument(
      "$text" -> BSONDocument("$search" -> q)
    )).cursor[Question]().gather[List]()
}

