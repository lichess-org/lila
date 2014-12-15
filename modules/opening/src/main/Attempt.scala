package lila.opening

import org.joda.time.DateTime

case class Attempt(
  id: String, // userId/openingId
  openingId: Opening.ID,
  userId: String,
  date: DateTime,
  score: Int)

object Attempt {

  def makeId(openingId: Opening.ID, userId: String) = s"$openingId/$userId"

  object BSONFields {
    val id = "_id"
    val openingId = "p"
    val userId = "u"
    val date = "d"
    val score = "s"
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
      score = r int score)

    def writes(w: BSON.Writer, o: Attempt) = BSONDocument(
      id -> o.id,
      openingId -> o.openingId,
      userId -> o.userId,
      date -> o.date,
      score -> o.score)
  }
}
