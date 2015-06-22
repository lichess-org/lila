package lila.opening

import org.joda.time.DateTime

case class Attempt(
    id: String, // userId/openingId
    openingId: Opening.ID,
    userId: String,
    win: Boolean,
    date: DateTime,
    openingRating: Int,
    openingRatingDiff: Int,
    userRating: Int,
    userRatingDiff: Int) {

  def loss = !win

  def userPostRating = userRating + userRatingDiff

  def openingPostRating = openingRating + openingRatingDiff
}

object Attempt {

  def makeId(openingId: Opening.ID, userId: String) = s"$openingId/$userId"

  object BSONFields {
    val id = "_id"
    val openingId = "p"
    val userId = "u"
    val win = "w"
    val date = "d"
    val openingRating = "or"
    val openingRatingDiff = "od"
    val userRating = "ur"
    val userRatingDiff = "ud"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val attemptBSONHandler = new BSON[Attempt] {

    import BSONFields._

    def reads(r: BSON.Reader): Attempt = Attempt(
      id = r str id,
      openingId = r int openingId,
      userId = r str userId,
      date = r.get[DateTime](date),
      win = r bool win,
      openingRating = r int openingRating,
      openingRatingDiff = r int openingRatingDiff,
      userRating = r int userRating,
      userRatingDiff = r int userRatingDiff)

    def writes(w: BSON.Writer, o: Attempt) = BSONDocument(
      id -> o.id,
      openingId -> o.openingId,
      userId -> o.userId,
      date -> o.date,
      win -> o.win,
      openingRating -> w.int(o.openingRating),
      openingRatingDiff -> w.int(o.openingRatingDiff),
      userRating -> w.int(o.userRating),
      userRatingDiff -> w.int(o.userRatingDiff))
  }
}
