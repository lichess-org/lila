package lila.activity

import reactivemongo.api.bson._
import scala.util.Success

import lila.common.{ Day, Iso }
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeKeyIso
import lila.rating.PerfType
import lila.study.BSONHandlers.StudyIdBSONHandler
import lila.study.Study
import lila.swiss.BsonHandlers.swissIdHandler
import lila.swiss.Swiss
import lila.user.User

private object BSONHandlers {

  import Activity._
  import activities._
  import model._

  def regexId(userId: User.ID): Bdoc = "_id" $startsWith s"$userId:"

  implicit lazy val activityIdHandler = {
    val sep = ':'
    tryHandler[Id](
      { case BSONString(v) =>
        v split sep match {
          case Array(userId, dayStr) => Success(Id(userId, Day(Integer.parseInt(dayStr))))
          case _                     => handlerBadValue(s"Invalid activity id $v")
        }
      },
      id => BSONString(s"${id.userId}$sep${id.day.value}")
    )
  }

  implicit private lazy val ratingHandler = BSONIntegerHandler.as[Rating](Rating.apply, _.value)
  implicit private lazy val ratingProgHandler = tryHandler[RatingProg](
    { case v: BSONArray =>
      for {
        before <- v.getAsTry[Rating](0)
        after  <- v.getAsTry[Rating](1)
      } yield RatingProg(before, after)
    },
    o => BSONArray(o.before, o.after)
  )

  implicit private lazy val scoreHandler = new lila.db.BSON[Score] {
    private val win  = "w"
    private val loss = "l"
    private val draw = "d"
    private val rp   = "r"

    def reads(r: lila.db.BSON.Reader) =
      Score(
        win = r.intD(win),
        loss = r.intD(loss),
        draw = r.intD(draw),
        rp = r.getO[RatingProg](rp)
      )

    def writes(w: lila.db.BSON.Writer, o: Score) =
      BSONDocument(
        win  -> w.intO(o.win),
        loss -> w.intO(o.loss),
        draw -> w.intO(o.draw),
        rp   -> o.rp
      )
  }

  implicit lazy val gamesHandler =
    typedMapHandler[PerfType, Score](perfTypeKeyIso)
      .as[Games](Games.apply, _.value)

  implicit private lazy val gameIdHandler = BSONStringHandler.as[GameId](GameId.apply, _.value)

  implicit private lazy val forumPostIdHandler = BSONStringHandler.as[ForumPostId](ForumPostId.apply, _.value)
  implicit lazy val forumPostsHandler =
    isoHandler[ForumPosts, List[ForumPostId]]((p: ForumPosts) => p.value, ForumPosts.apply _)

  implicit private lazy val ublogPostIdHandler = BSONStringHandler.as[UblogPostId](UblogPostId.apply, _.value)
  implicit lazy val ublogPostsHandler =
    isoHandler[UblogPosts, List[UblogPostId]]((p: UblogPosts) => p.value, UblogPosts.apply _)

  implicit lazy val puzzlesHandler = isoHandler[Puzzles, Score]((p: Puzzles) => p.score, Puzzles.apply _)

  implicit lazy val stormHandler = new lila.db.BSON[Storm] {
    def reads(r: lila.db.BSON.Reader)            = Storm(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, s: Storm) = BSONDocument("r" -> s.runs, "s" -> s.score)
  }

  implicit lazy val racerHandler = new lila.db.BSON[Racer] {
    def reads(r: lila.db.BSON.Reader)            = Racer(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Racer) = BSONDocument("r" -> r.runs, "s" -> r.score)
  }

  implicit lazy val streakHandler = new lila.db.BSON[Streak] {
    def reads(r: lila.db.BSON.Reader)             = Streak(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Streak) = BSONDocument("r" -> r.runs, "s" -> r.score)
  }

  implicit private lazy val learnHandler =
    typedMapHandler[Learn.Stage, Int](Iso.string(Learn.Stage.apply, _.value))
      .as[Learn](Learn.apply, _.value)

  implicit private lazy val practiceHandler =
    typedMapHandler[Study.Id, Int](Iso.string[Study.Id](Study.Id.apply, _.value))
      .as[Practice](Practice.apply, _.value)

  implicit private lazy val simulIdHandler = BSONStringHandler.as[SimulId](SimulId.apply, _.value)
  implicit private lazy val simulsHandler =
    isoHandler[Simuls, List[SimulId]]((s: Simuls) => s.value, Simuls.apply _)

  implicit lazy val corresHandler         = Macros.handler[Corres]
  implicit private lazy val patronHandler = BSONIntegerHandler.as[Patron](Patron.apply, _.months)

  implicit private lazy val followListHandler = Macros.handler[FollowList]

  implicit private lazy val followsHandler = new lila.db.BSON[Follows] {
    def reads(r: lila.db.BSON.Reader) =
      Follows(
        in = r.getO[FollowList]("i").filterNot(_.isEmpty),
        out = r.getO[FollowList]("o").filterNot(_.isEmpty)
      )
    def writes(w: lila.db.BSON.Writer, o: Follows) =
      BSONDocument(
        "i" -> o.in,
        "o" -> o.out
      )
  }

  implicit private lazy val studiesHandler =
    isoHandler[Studies, List[Study.Id]]((s: Studies) => s.value, Studies.apply _)
  implicit private lazy val teamsHandler =
    isoHandler[Teams, List[String]]((s: Teams) => s.value, Teams.apply _)

  implicit lazy val swissRankHandler = new lila.db.BSON[SwissRank] {
    def reads(r: lila.db.BSON.Reader)                = SwissRank(Swiss.Id(r.str("i")), r.intD("r"))
    def writes(w: lila.db.BSON.Writer, s: SwissRank) = BSONDocument("i" -> s.id, "r" -> s.rank)
  }
  implicit private lazy val swissesHandler =
    isoHandler[Swisses, List[SwissRank]]((s: Swisses) => s.value, Swisses.apply _)

  object ActivityFields {
    val id         = "_id"
    val games      = "g"
    val forumPosts = "p"
    val ublogPosts = "u"
    val puzzles    = "z"
    val storm      = "m"
    val racer      = "c"
    val streak     = "k"
    val learn      = "l"
    val practice   = "r"
    val simuls     = "s"
    val corres     = "o"
    val patron     = "a"
    val follows    = "f"
    val studies    = "t"
    val teams      = "e"
    val swisses    = "w"
    val stream     = "st"
  }

  implicit lazy val activityHandler = new lila.db.BSON[Activity] {

    import ActivityFields._

    def reads(r: lila.db.BSON.Reader) =
      Activity(
        id = r.get[Id](id),
        games = r.getO[Games](games),
        forumPosts = r.getO[ForumPosts](forumPosts),
        puzzles = r.getO[Puzzles](puzzles),
        storm = r.getO[Storm](storm),
        racer = r.getO[Racer](racer),
        streak = r.getO[Streak](streak),
        learn = r.getO[Learn](learn),
        practice = r.getO[Practice](practice),
        simuls = r.getO[Simuls](simuls),
        corres = r.getO[Corres](corres),
        patron = r.getO[Patron](patron),
        follows = r.getO[Follows](follows).filterNot(_.isEmpty),
        studies = r.getO[Studies](studies),
        teams = r.getO[Teams](teams),
        swisses = r.getO[Swisses](swisses),
        stream = r.getD[Boolean](stream)
      )

    def writes(w: lila.db.BSON.Writer, o: Activity) =
      BSONDocument(
        id         -> o.id,
        games      -> o.games,
        forumPosts -> o.forumPosts,
        puzzles    -> o.puzzles,
        storm      -> o.storm,
        racer      -> o.racer,
        streak     -> o.streak,
        learn      -> o.learn,
        practice   -> o.practice,
        simuls     -> o.simuls,
        corres     -> o.corres,
        patron     -> o.patron,
        follows    -> o.follows,
        studies    -> o.studies,
        teams      -> o.teams,
        swisses    -> o.swisses,
        stream     -> o.stream.option(true)
      )
  }
}
