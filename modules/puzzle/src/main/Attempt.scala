package lila.puzzle

import org.joda.time.DateTime

case class Attempt(
  id: String, // userId/puzzleId
  puzzleId: PuzzleId,
  userId: String,
  date: DateTime,
  win: Boolean,
  hints: Int,
  puzzleRating: Int,
  userRating: Int,
  vote: Option[Boolean])

object Attempt {

  def makeId(puzzleId: String, userId: String) = s"$puzzleId/$userId"

  object BSONFields {
    val id = "_id"
    val puzzleId = "p"
    val userId = "u"
    val date = "d"
    val win = "s"
    val hints = "h"
    val puzzleRating = "pr"
    val userRating = "ur"
    val vote = "v"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val attemptBSONHandler = new BSON[Attempt] {

    import BSONFields._

    def reads(r: BSON.Reader): Attempt = Attempt(
      id = r str id,
      puzzleId = r str puzzleId,
      userId = r str userId,
      date = r.get[DateTime](date),
      win = r boolD win,
      hints = r intD hints,
      puzzleRating = r int puzzleRating,
      userRating = r int userRating,
      vote = r boolO vote)

    def writes(w: BSON.Writer, o: Attempt) = BSONDocument(
      id -> o.id,
      puzzleId -> o.puzzleId,
      userId -> o.userId,
      date -> o.date,
      win -> w.boolO(o.win),
      hints -> w.intO(o.hints),
      puzzleRating -> o.puzzleRating,
      userRating -> o.userRating,
      vote -> o.vote)
  }
}
