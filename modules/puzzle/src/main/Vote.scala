package lila.puzzle

import org.joda.time.DateTime

case class Vote(
    id: String, // userId/puzzleId
    vote: Boolean)

object Vote {

  def makeId(puzzleId: PuzzleId, userId: String) = s"$puzzleId/$userId"

  object BSONFields {
    val id = "_id"
    val vote = "v"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val voteBSONHandler = new BSON[Vote] {

    import BSONFields._

    def reads(r: BSON.Reader): Vote = Vote(
      id = r str id,
      vote = r bool vote)

    def writes(w: BSON.Writer, o: Vote) = BSONDocument(
      id -> o.id,
      vote -> o.vote)
  }
}
