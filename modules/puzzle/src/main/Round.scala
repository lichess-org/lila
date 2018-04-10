package lila.puzzle

import org.joda.time.DateTime

import lila.user.User

case class Round(
    id: Round.Id,
    date: DateTime,
    result: Result,
    rating: Int,
    ratingDiff: Int
) {

  def userPostRating = rating + ratingDiff
}

object Round {

  case class Id(userId: User.ID, puzzleId: PuzzleId)

  object BSONFields {
    val id = "_id"
    val date = "a"
    val magic = "m"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import lila.db.dsl._
  import BSON.BSONJodaDateTimeHandler

  private implicit val ResultBSONHandler = booleanAnyValHandler[Result](_.win, Result.apply)

  /* We shift the puzzle ID by -60000
     * Because the initial puzzle is 60000 and something.
     * This way the lowest puzzle ID is 00000
     * and it allows us to sort rounds by ID: userId:puzzleId
     * because all puzzle IDs have the same length */
  private val shiftValue = -60000
  def encode(puzzleId: PuzzleId) = puzzleId + shiftValue
  def decode(puzzleId: PuzzleId) = puzzleId - shiftValue

  implicit val roundIdHandler: BSONHandler[BSONString, Id] = new BSONHandler[BSONString, Id] {
    private val sep = ':'
    def read(bs: BSONString) = bs.value split sep match {
      case Array(userId, puzzleId) => Id(userId, decode(Integer parseInt puzzleId))
      case _ => sys error s"Invalid puzzle round id ${bs.value}"
    }
    def write(id: Id) = {
      val puzzleId = "%05d".format(encode(id.puzzleId))
      BSONString(s"${id.userId}$sep$puzzleId")
    }
  }

  implicit val RoundBSONHandler = new BSON[Round] {

    import BSONFields._

    /* `magic` field stores;
     * - win: boolean     | 1 bit
     * - ratingDiff: int  | 15 bits
     * - rating: int      | 16 bits
     */

    def reads(r: BSON.Reader): Round = {
      val m = r int magic
      val win = m >>> 31 != 0
      val ratingDiff = (m << 1) >>> 17
      Round(
        id = r.get[Id](id),
        date = r.get[DateTime](date),
        result = Result(win),
        rating = m & (-1 >>> 16),
        ratingDiff = if (win) ratingDiff else -ratingDiff
      )
    }

    def writes(w: BSON.Writer, o: Round) = BSONDocument(
      id -> o.id,
      date -> o.date,
      magic -> {
        (o.result.win ?? (1 << 31)) |
          (Math.abs(o.ratingDiff) << 16) |
          o.rating
      }
    )
  }
}
