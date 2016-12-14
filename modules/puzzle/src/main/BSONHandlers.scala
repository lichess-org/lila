package lila.puzzle

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._

private object BSONHandlers {

  implicit val tagBSONHandler = new BSONHandler[BSONDocument, List[TagVoted]] {
    def read(doc: BSONDocument): List[TagVoted] = ??? /*doc.elements.toList map {
      case BSONDocument(id -> BSONDocument("up" -> up, "down" -> down)) => TagVoted(Tag.byId(id), TagAggregateVote(up, down))
      case _ => throw new Exception(s"malformed BSONDocument")
    }*/
    def write(tags: List[TagVoted]): BSONDocument = ???
      // BSONDocument(tags map {
      //   t => BSONDocument(t.tag.id -> BSONDocument("up" -> t.vote.up, "down" -> t.vote.down))
      // })
  }
}
