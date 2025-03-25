package lila.game

import scala.util.Success

case class Crosstable(
    users: Crosstable.Users,
    results: List[Crosstable.Result] // chronological order, oldest to most recent
):
  export users.{ fromPov as _, * }

  def nonEmpty                = results.nonEmpty.option(this)
  def fromPov(userId: UserId) = copy(users = users.fromPov(userId))

  lazy val size = results.size

  def fillSize = Crosstable.maxGames - size

object Crosstable:

  val maxGames = 20

  def empty(u1: UserId, u2: UserId) =
    Crosstable(
      Users(User(u1, 0), User(u2, 0)),
      Nil
    )

  case class User(id: UserId, score: Int) // score is x10
  case class Users(user1: User, user2: User):

    val nbGames = (user1.score + user2.score) / 10

    def user(id: UserId): Option[User] =
      if id == user1.id then Some(user1)
      else if id == user2.id then Some(user2)
      else None

    def toList = List(user1, user2)

    def showScore(userId: UserId) =
      val byTen = user(userId).so(_.score)
      s"${byTen / 10}${(byTen % 10 != 0).so("½")}" match
        case "0½" => "½"
        case x    => x

    def showOpponentScore(userId: UserId) =
      if userId == user1.id then showScore(user2.id).some
      else if userId == user2.id then showScore(user1.id).some
      else none

    def fromPov(userId: UserId) =
      if userId == user2.id then copy(user1 = user2, user2 = user1)
      else this

    def winnerId =
      if user1.score > user2.score then Some(user1.id)
      else if user1.score < user2.score then Some(user2.id)
      else None

  case class Result(gameId: GameId, winnerId: Option[UserId])

  case class Matchup(users: Users): // score is x10
    def fromPov(userId: UserId) = copy(users = users.fromPov(userId))
    def nonEmpty                = users.nbGames > 0

  case class WithMatchup(crosstable: Crosstable, matchup: Option[Matchup]):
    def fromPov(userId: UserId) =
      copy(
        crosstable.fromPov(userId),
        matchup.map(_.fromPov(userId))
      )

  private[game] def makeKey(u1: UserId, u2: UserId): String =
    if u1.value < u2.value then s"$u1/$u2" else s"$u2/$u1"

  import reactivemongo.api.bson.*
  import lila.db.BSON
  import lila.db.dsl.*

  object BSONFields:
    val id         = "_id"
    val score1     = "s1"
    val score2     = "s2"
    val results    = "r"
    val lastPlayed = "d"

  private[game] given crosstableHandler: BSON[Crosstable] with
    import BSONFields.*
    def reads(r: BSON.Reader): Crosstable =
      r.str(id).split('/') match
        case Array(u1Id, u2Id) =>
          Crosstable(
            users = Users(User(UserId(u1Id), r.intD(score1)), User(UserId(u2Id), r.intD(score2))),
            results = r.get[List[String]](results).map { r =>
              r.drop(8) match
                case ""  => Result(GameId(r), Some(UserId(u1Id)))
                case "-" => Result(GameId(r.take(8)), Some(UserId(u2Id)))
                case "=" => Result(GameId(r.take(8)), none)
                case _   => sys.error(s"Invalid result string $r")
            }
          )
        case x => sys.error(s"Invalid crosstable id $x")
    def writeResult(result: Result, u1: UserId): String =
      val flag = result.winnerId match
        case Some(wid) if wid == u1 => ""
        case Some(_)                => "-"
        case None                   => "="
      s"${result.gameId}$flag"
    def writes(w: BSON.Writer, o: Crosstable) =
      BSONDocument(
        id      -> makeKey(o.user1.id, o.user2.id),
        score1  -> o.user1.score,
        score2  -> o.user2.score,
        results -> o.results.map { writeResult(_, o.user1.id) }
      )

  private[game] given BSONDocumentReader[Matchup] with
    import BSONFields.*
    def readDocument(doc: Bdoc) =
      val r = new BSON.Reader(doc)
      r.str(id).split('/') match
        case Array(u1Id, u2Id) =>
          Success:
            Matchup(Users(User(UserId(u1Id), r.intD(score1)), User(UserId(u2Id), r.intD(score2))))
        case x => lila.db.BSON.handlerBadValue(s"Invalid crosstable id $x")
