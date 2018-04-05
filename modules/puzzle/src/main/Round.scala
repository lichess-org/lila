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

  implicit val roundIdHandler: BSONHandler[BSONString, Id] = new BSONHandler[BSONString, Id] {
    private val sep = ':'
    /* We shift the puzzle ID by -60000
     * Because the initial puzzle is 60000 and something.
     * This way the lowest puzzle ID is 00000
     * and it allows us to sort rounds by ID: userId:puzzleId
     * because all puzzle IDs have the same length */
    private val shift = 60000
    def read(bs: BSONString) = bs.value split sep match {
      case Array(userId, puzzleId) => Id(userId, Integer.parseInt(puzzleId) + shift)
      case _ => sys error s"Invalid puzzle round id ${bs.value}"
    }
    def write(id: Id) = {
      val puzzleId = "%05d".format(id.puzzleId - shift)
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

    private val winBit = 1 << 31
    private val cancelWinBit = ((1 << 30) - 1)

    def reads(r: BSON.Reader): Round = {
      val m = r int magic
      val win = (m >>> 31) == 1
      Round(
        id = r.get[Id](id),
        date = r.get[DateTime](date),
        result = Result(win),
        rating = m << 16 >>> 16,
        ratingDiff = ((m & cancelWinBit) >> 16) * (if (win) 1 else -1)
      )
    }

    def writes(w: BSON.Writer, o: Round) = BSONDocument(
      id -> o.id,
      date -> o.date,
      magic -> {
        (o.result.win ?? winBit) +
          (Math.abs(o.ratingDiff) << 16) +
          o.rating
      }
    )
  }
}
