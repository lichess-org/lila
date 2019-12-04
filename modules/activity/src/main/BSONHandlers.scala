package lila.activity

import reactivemongo.api.bson._
import scala.util.Success

import lila.common.Iso
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeKeyIso
import lila.rating.PerfType
import lila.study.BSONHandlers._
import lila.study.Study
import lila.user.User

private object BSONHandlers {

  import Activity._
  import activities._
  import model._

  def regexId(userId: User.ID): Bdoc = "_id" $startsWith s"$userId:"

  implicit val activityIdHandler = {
    val sep = ':'
    tryHandler[Id](
      {
        case BSONString(v) => v split sep match {
          case Array(userId, dayStr) => Success(Id(userId, Day(Integer.parseInt(dayStr))))
          case _ => handlerBadValue(s"Invalid activity id $v")
        }
      },
      id => BSONString(s"${id.userId}$sep${id.day.value}")
    )
  }

  private implicit val ratingHandler = BSONIntegerHandler.as[Rating](Rating.apply, _.value)
  private implicit val ratingProgHandler = tryHandler[RatingProg](
    {
      case v: BSONArray => for {
        before <- v.getAsTry[Rating](0)
        after <- v.getAsTry[Rating](1)
      } yield RatingProg(before, after)
    },
    o => BSONArray(o.before, o.after)
  )

  private implicit val scoreHandler: BSONHandler[Score] = new lila.db.BSON[Score] {
    private val win = "w"
    private val loss = "l"
    private val draw = "d"
    private val rp = "r"

    def reads(r: lila.db.BSON.Reader) = Score(
      win = r.intD(win),
      loss = r.intD(loss),
      draw = r.intD(draw),
      rp = r.getO[RatingProg](rp)
    )

    def writes(w: lila.db.BSON.Writer, o: Score) = BSONDocument(
      win -> w.intO(o.win),
      loss -> w.intO(o.loss),
      draw -> w.intO(o.draw),
      rp -> o.rp
    )
  }

  private implicit val perfTypeHandler: BSONHandler[PerfType] = isoHandler(perfTypeKeyIso)
  private implicit val gameMapHandler: BSONHandler[Map[PerfType, Score]] = implicitly[BSONHandler[Map[PerfType, Score]]]
  implicit val gamesHandler = gameMapHandler.as[Games](Games.apply, _.value)

  private implicit val gameIdHandler = BSONStringHandler.as[GameId](GameId.apply, _.value)

  private implicit val postIdHandler = BSONStringHandler.as[PostId](PostId.apply, _.value)
  implicit val postsHandler = isoHandler[Posts, List[PostId]]((p: Posts) => p.value, Posts.apply _)

  implicit val puzzlesHandler = isoHandler[Puzzles, Score]((p: Puzzles) => p.score, Puzzles.apply _)

  private implicit val learnMapHandler: BSONHandler[Map[Learn.Stage, Int]] = implicitly[BSONHandler[Map[Learn.Stage, Int]]]
  private implicit val learnHandler = learnMapHandler.as[Learn](Learn.apply, _.value)

  private implicit val practiceMapHandler: BSONHandler[Map[Study.Id, Int]] = implicitly[BSONHandler[Map[Study.Id, Int]]]
  private implicit val practiceHandler = practiceMapHandler.as[Practice](Practice.apply, _.value)

  private implicit val simulIdHandler = BSONStringHandler.as[SimulId](SimulId.apply, _.value)
  private implicit val simulsHandler = isoHandler[Simuls, List[SimulId]]((s: Simuls) => s.value, Simuls.apply _)

  implicit val corresHandler = Macros.handler[Corres]
  private implicit val patronHandler = BSONIntegerHandler.as[Patron](Patron.apply, _.months)

  private implicit val followListHandler = Macros.handler[FollowList]

  private implicit val followsHandler = new lila.db.BSON[Follows] {
    def reads(r: lila.db.BSON.Reader) = Follows(
      in = r.getO[FollowList]("i").filterNot(_.isEmpty),
      out = r.getO[FollowList]("o").filterNot(_.isEmpty)
    )
    def writes(w: lila.db.BSON.Writer, o: Follows) = BSONDocument(
      "i" -> o.in,
      "o" -> o.out
    )
  }

  private implicit val studiesHandler = isoHandler[Studies, List[Study.Id]]((s: Studies) => s.value, Studies.apply _)
  private implicit val teamsHandler = isoHandler[Teams, List[String]]((s: Teams) => s.value, Teams.apply _)

  object ActivityFields {
    val id = "_id"
    val games = "g"
    val posts = "p"
    val puzzles = "z"
    val learn = "l"
    val practice = "r"
    val simuls = "s"
    val corres = "o"
    val patron = "a"
    val follows = "f"
    val studies = "t"
    val teams = "e"
    val stream = "st"
  }

  implicit val activityHandler = new lila.db.BSON[Activity] {

    import ActivityFields._

    def reads(r: lila.db.BSON.Reader) = Activity(
      id = r.get[Id](id),
      games = r.getO[Games](games),
      posts = r.getO[Posts](posts),
      puzzles = r.getO[Puzzles](puzzles),
      learn = r.getO[Learn](learn),
      practice = r.getO[Practice](practice),
      simuls = r.getO[Simuls](simuls),
      corres = r.getO[Corres](corres),
      patron = r.getO[Patron](patron),
      follows = r.getO[Follows](follows).filterNot(_.isEmpty),
      studies = r.getO[Studies](studies),
      teams = r.getO[Teams](teams),
      stream = r.getD[Boolean](stream)
    )

    def writes(w: lila.db.BSON.Writer, o: Activity) = BSONDocument(
      id -> o.id,
      games -> o.games,
      posts -> o.posts,
      puzzles -> o.puzzles,
      learn -> o.learn,
      practice -> o.practice,
      simuls -> o.simuls,
      corres -> o.corres,
      patron -> o.patron,
      follows -> o.follows,
      studies -> o.studies,
      teams -> o.teams,
      stream -> o.stream.option(true)
    )
  }
}
