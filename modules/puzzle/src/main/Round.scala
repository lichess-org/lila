package lidraughts.puzzle

import org.joda.time.DateTime

import lidraughts.user.User

case class Round(
    puzzleId: PuzzleId,
    userId: User.ID,
    date: DateTime,
    result: Result,
    rating: Int,
    ratingDiff: Int
) {

  def userPostRating = rating + ratingDiff
}

object Round {

  case class Mini(puzzleId: Int, ratingDiff: Int, rating: Int)

  object BSONFields {
    val puzzleId = "p"
    val userId = "u"
    val date = "a"
    val result = "w"
    val rating = "r"
    val ratingDiff = "d"
  }

  import reactivemongo.bson._
  import lidraughts.db.BSON
  import lidraughts.db.dsl._
  import BSON.BSONJodaDateTimeHandler

  private implicit val ResultBSONHandler = booleanAnyValHandler[Result](_.win, Result.apply)

  implicit val RoundBSONHandler = new BSON[Round] {

    import BSONFields._

    def reads(r: BSON.Reader): Round = Round(
      puzzleId = r int puzzleId,
      userId = r str userId,
      date = r.get[DateTime](date),
      result = r.get[Result](result),
      rating = r int rating,
      ratingDiff = r int ratingDiff
    )

    def writes(w: BSON.Writer, o: Round) = BSONDocument(
      puzzleId -> o.puzzleId,
      userId -> o.userId,
      date -> o.date,
      result -> o.result,
      rating -> w.int(o.rating),
      ratingDiff -> w.int(o.ratingDiff)
    )
  }

  private[puzzle] implicit val RoundMiniBSONReader = new BSONDocumentReader[Mini] {
    import BSONFields._
    def read(doc: Bdoc): Mini = Mini(
      puzzleId = doc.getAs[Int](puzzleId) err "RoundMini no puzzleId",
      rating = doc.getAs[Int](rating) err "RoundMini no rating",
      ratingDiff = doc.getAs[Int](ratingDiff) err "RoundMini no ratingDiff"
    )
  }
}
