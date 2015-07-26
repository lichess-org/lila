package lila.game

case class Crosstable(
    user1: Crosstable.User,
    user2: Crosstable.User,
    results: List[Crosstable.Result],
    nbGames: Int) {

  import Crosstable.Result

  def nonEmpty = results.nonEmpty option this

  def users = List(user2, user1)

  def winnerId =
    if (user1.score > user2.score) Some(user1.id)
    else if (user1.score < user2.score) Some(user2.id)
    else None

  def user(id: String) = users find (_.id == id)

  def showScore(userId: String) = {
    val byTen = user(userId) ?? (_.score)
    s"${byTen / 10}${(byTen % 10 != 0).??("½")}" match {
      case "0½" => "½"
      case x    => x
    }
  }

  def showOpponentScore(userId: String) =
    if (userId == user1.id) showScore(user2.id).some
    else if (userId == user2.id) showScore(user1.id).some
    else none

  def addWins(userId: Option[String], wins: Int) = copy(
    user1 = user1.copy(
      score = user1.score + (userId match {
        case None                     => wins * 5
        case Some(u) if user1.id == u => wins * 10
        case _                        => 0
      })),
    user2 = user2.copy(
      score = user2.score + (userId match {
        case None                     => wins * 5
        case Some(u) if user2.id == u => wins * 10
        case _                        => 0
      })))

  def fromPov(userId: String) =
    if (userId == user2.id) copy(user1 = user2, user2 = user1)
    else this

  lazy val size = results.size

  def fill = (1 to 20 - size)
}

object Crosstable {

  case class User(id: String, score: Int) // score is x10
  case class Result(gameId: String, winnerId: Option[String])

  private[game] def makeKey(u1: String, u2: String): String = List(u1, u2).sorted mkString "/"

  import reactivemongo.bson._
  import lila.db.BSON

  object BSONFields {
    val id = "_id"
    val score1 = "s1"
    val score2 = "s2"
    val results = "r"
    val nbGames = "n"
  }

  implicit val crosstableBSONHandler = new BSON[Crosstable] {

    import BSONFields._

    def reads(r: BSON.Reader): Crosstable = r str id split '/' match {
      case Array(u1Id, u2Id) => Crosstable(
        user1 = User(u1Id, r intD "s1"),
        user2 = User(u2Id, r intD "s2"),
        results = r.get[List[String]](results).map { r =>
          r drop 8 match {
            case ""  => Result(r take 8, none)
            case "+" => Result(r take 8, Some(u1Id))
            case "-" => Result(r take 8, Some(u2Id))
            case _   => sys error s"Invalid result string $r"
          }
        },
        nbGames = r int nbGames)
      case x => sys error s"Invalid crosstable id $x"
    }

    def writeResult(result: Result, u1: String): String =
      result.gameId + (result.winnerId ?? { w => if (w == u1) "+" else "-" })

    def writes(w: BSON.Writer, o: Crosstable) = BSONDocument(
      id -> makeKey(o.user1.id, o.user2.id),
      score1 -> o.user1.score,
      score2 -> o.user2.score,
      results -> o.results.map { writeResult(_, o.user1.id) },
      nbGames -> w.int(o.nbGames))
  }
}
