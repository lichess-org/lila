package lila.activity

import reactivemongo.api.bson.*
import scala.util.Success

import lila.common.{ Iso, LichessDay }
import lila.db.dsl.{ *, given }
import lila.rating.BSONHandlers.given
import lila.rating.PerfType
import lila.study.BSONHandlers.given
import lila.study.Study
import lila.swiss.BsonHandlers.given
import lila.swiss.Swiss
import lila.user.User

private object BSONHandlers:

  import Activity.*
  import activities.*
  import model.*

  val idSep                          = ':'
  def regexId(userId: User.ID): Bdoc = "_id" $startsWith s"$userId$idSep"

  given BSONHandler[Id] = tryHandler(
    { case BSONString(v) =>
      v split idSep match {
        case Array(userId, dayStr) => Success(Id(userId, LichessDay(Integer.parseInt(dayStr))))
        case _                     => handlerBadValue(s"Invalid activity id $v")
      }
    },
    id => BSONString(s"${id.userId}$idSep${id.day.value}")
  )

  private given BSONHandler[Rating] = BSONIntegerHandler.as(Rating, _.value)
  private given BSONHandler[RatingProg] = tryHandler(
    { case v: BSONArray =>
      for {
        before <- v.getAsTry[Rating](0)
        after  <- v.getAsTry[Rating](1)
      } yield RatingProg(before, after)
    },
    o => BSONArray(o.before, o.after)
  )

  private given lila.db.BSON[Score] with
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

  private[activity] given BSONHandler[Games] = typedMapHandler[PerfType, Score].as(Games, _.value)

  private given BSONHandler[GameId] = BSONStringHandler.as(GameId, _.value)

  private given BSONHandler[ForumPostId] = BSONStringHandler.as(ForumPostId, _.value)
  given BSONHandler[ForumPosts]          = isoHandler[ForumPosts, List[ForumPostId]](_.value, ForumPosts)

  private given BSONHandler[UblogPostId] = BSONStringHandler.as(UblogPostId, _.value)
  given BSONHandler[UblogPosts]          = isoHandler[UblogPosts, List[UblogPostId]](_.value, UblogPosts)

  given BSONHandler[Puzzles] = isoHandler[Puzzles, Score](_.score, Puzzles)

  given lila.db.BSON[Storm] with
    def reads(r: lila.db.BSON.Reader)            = Storm(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, s: Storm) = BSONDocument("r" -> s.runs, "s" -> s.score)

  given lila.db.BSON[Racer] with
    def reads(r: lila.db.BSON.Reader)            = Racer(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Racer) = BSONDocument("r" -> r.runs, "s" -> r.score)

  given lila.db.BSON[Streak] with
    def reads(r: lila.db.BSON.Reader)             = Streak(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Streak) = BSONDocument("r" -> r.runs, "s" -> r.score)

  private given Iso.StringIso[Learn.Stage] = Iso.string(Learn.Stage, _.value)
  given BSONHandler[Learn]                 = typedMapHandler[Learn.Stage, Int].as(Learn, _.value)

  private given Iso.StringIso[Study.Id] = Iso.string(Study.Id, _.value)
  given BSONHandler[Practice]           = typedMapHandler[Study.Id, Int].as[Practice](Practice, _.value)

  given BSONHandler[SimulId] = BSONStringHandler.as(SimulId, _.value)
  given BSONHandler[Simuls]  = isoHandler[Simuls, List[SimulId]](_.value, Simuls)

  given BSONDocumentHandler[Corres] = Macros.handler
  given BSONHandler[Patron]         = BSONIntegerHandler.as(Patron, _.months)

  given BSONDocumentHandler[FollowList] = Macros.handler[FollowList]

  given lila.db.BSON[Follows] with
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

  given BSONHandler[Studies] = isoHandler[Studies, List[Study.Id]](_.value, Studies)
  given BSONHandler[Teams]   = isoHandler[Teams, List[String]](_.value, Teams)

  given lila.db.BSON[SwissRank] with
    def reads(r: lila.db.BSON.Reader)                = SwissRank(Swiss.Id(r.str("i")), r.intD("r"))
    def writes(w: lila.db.BSON.Writer, s: SwissRank) = BSONDocument("i" -> s.id, "r" -> s.rank)

  given BSONHandler[Swisses] = isoHandler[Swisses, List[SwissRank]](_.value, Swisses)

  object ActivityFields:
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

  given lila.db.BSON[Activity] with

    import ActivityFields.*

    def reads(r: lila.db.BSON.Reader) = Activity(
      id = r.get[Id](id),
      games = r.getO[Games](games),
      forumPosts = r.getO[ForumPosts](forumPosts),
      ublogPosts = r.getO[UblogPosts](ublogPosts),
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

    def writes(w: lila.db.BSON.Writer, o: Activity) = BSONDocument(
      id         -> o.id,
      games      -> o.games,
      forumPosts -> o.forumPosts,
      ublogPosts -> o.ublogPosts,
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
