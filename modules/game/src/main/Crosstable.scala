package lila.game

case class Crosstable(
    user1: String,
    user2: String,
    results: List[Crosstable.Result],
    nbGames: Int) {

  import Crosstable.Result

  def nonEmpty = results.nonEmpty option this

  def userIds = List(user1, user2)

  def score(u: String) = if (u == user1) score1 else score2

  private lazy val score1 = computeScore(user1)
  private lazy val score2 = computeScore(user2)

  // multiplied by ten
  private def computeScore(userId: String): Int = results.foldLeft(0) {
    case (s, Result(_, Some(w))) if w == userId => s + 10
    case (s, Result(_, None))                   => s + 5
    case (s, _)                                 => s
  }

  def winnerId =
    if (score1 > score2) Some(user1)
    else if (score1 < score2) Some(user2)
    else None

  def showScore(byTen: Int) = s"${byTen / 10}${(byTen % 10 != 0).??("½")}"
}

object Crosstable {

  case class Result(gameId: String, winnerId: Option[String])

  private[game] def makeKey(u1: String, u2: String): String = List(u1, u2).sorted mkString "/"

  import reactivemongo.bson._
  import lila.db.BSON

  object BSONFields {

    val id = "_id"
    val results = "r"
    val nbGames = "n"
  }

  implicit val crosstableBSONHandler = new BSON[Crosstable] {

    import BSONFields._

    def reads(r: BSON.Reader): Crosstable = r str id split '/' match {
      case Array(u1, u2) => Crosstable(
        user1 = u1,
        user2 = u2,
        results = r.get[List[String]](results).map {
          _ split '/' match {
            case Array(gameId, res) => Result(gameId, Some(if (res == "1") u1 else u2))
            case Array(gameId)      => Result(gameId, none)
            case x                  => sys error s"Invalid result string $x"
          }
        },
        nbGames = r int nbGames)
      case x => sys error s"Invalid crosstable id $x"
    }

    def writeResult(result: Result, u1: String): String = {
      val res = result.winnerId ?? { w => s"/${if (w == u1) 1 else 0}" }
      s"${result.gameId}$res"
    }

    def writes(w: BSON.Writer, o: Crosstable) = BSONDocument(
      id -> makeKey(o.user1, o.user2),
      results -> o.results.map { writeResult(_, o.user1) },
      nbGames -> w.int(o.nbGames))
  }
}
