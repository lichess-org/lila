package lila.puzzle

import org.joda.time.DateTime

import lila.user.User

case class Round(
    puzzleId: PuzzleId,
    userId: User.ID,
    date: DateTime,
    win: Boolean,
    rating: Int,
    ratingDiff: Int) {

  def loss = !win

  def userPostRating = rating + ratingDiff
}

object Round {

  object BSONFields {
    val puzzleId = "p"
    val userId = "u"
    val date = "d"
    val win = "w"
    val rating = "r"
    val ratingDiff = "d"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val roundBSONHandler = new BSON[Round] {

    import BSONFields._

    def reads(r: BSON.Reader): Round = Round(
      puzzleId = r int puzzleId,
      userId = r str userId,
      date = r.get[DateTime](date),
      win = r bool win,
      rating = r int rating,
      ratingDiff = r int ratingDiff)

    def writes(w: BSON.Writer, o: Round) = BSONDocument(
      puzzleId -> o.puzzleId,
      userId -> o.userId,
      date -> o.date,
      win -> o.win,
      rating -> w.int(o.rating),
      ratingDiff -> w.int(o.ratingDiff))
  }
}
