package lila.puzzle

import org.specs2.mutable._
import org.specs2.specification._

import reactivemongo.bson._

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._

class BSONHandlersTest extends Specification {

  "TagVoteds" should {
    import BSONHandlers.tagVotedsBSONHandler._
    "write" in {
      val scala = TagVoteds(List(
        TagVoted(
          Tag.Fork,
          TagAggregateVote(5, 2)),
        TagVoted(
          Tag.Overload,
          TagAggregateVote(10, 3))
      ))
      val mongo = $doc(
        "fork" -> $doc(
          "up" -> 5,
          "down" -> 2
        ),
        "overload" -> $doc(
          "up" -> 10,
          "down" -> 3
        ))
      write(scala) must_== mongo
    }
  }
}
