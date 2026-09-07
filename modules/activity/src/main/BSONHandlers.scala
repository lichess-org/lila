package lila.activity

import reactivemongo.api.bson.*
import scala.util.Success
import chess.IntRating

import lila.common.LichessDay
import lila.core.chess.Rank
import lila.core.rating.{ RatingProg, Score }
import lila.db.dsl.{ *, given }

private object BSONHandlers:

  import Activity.*
  import activities.*

  val idSep = ':'
  def regexId(userId: UserId): Bdoc = "_id".$startsWith(s"$userId$idSep")

  given BSONHandler[Id] = tryHandler(
    { case BSONString(v) =>
      v.split(idSep) match
        case Array(userId, dayStr) => Success(Id(UserId(userId), LichessDay(Integer.parseInt(dayStr))))
        case _ => handlerBadValue(s"Invalid activity id $v")
    },
    id => BSONString(s"${id.userId}$idSep${id.day.value}")
  )

  private given BSONHandler[RatingProg] = tryHandler(
    { case v: BSONArray =>
      for
        before <- v.getAsTry[IntRating](0)
        after <- v.getAsTry[IntRating](1)
      yield RatingProg(before, after)
    },
    o => BSONArray(o.before, o.after)
  )

  private[activity] given lila.db.BSON[Score] with
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

  private[activity] given BSONHandler[Games] =
    typedMapHandlerIso[PerfKey, Score].as(Games(_), _.value)

  given lila.db.BSON[Storm] with
    def reads(r: lila.db.BSON.Reader) = Storm(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, s: Storm) = BSONDocument("r" -> s.runs, "s" -> s.score)

  given lila.db.BSON[Racer] with
    def reads(r: lila.db.BSON.Reader) = Racer(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Racer) = BSONDocument("r" -> r.runs, "s" -> r.score)

  given lila.db.BSON[Streak] with
    def reads(r: lila.db.BSON.Reader) = Streak(r.intD("r"), r.intD("s"))
    def writes(w: lila.db.BSON.Writer, r: Streak) = BSONDocument("r" -> r.runs, "s" -> r.score)

  given BSONHandler[Learn] = typedMapHandler[LearnStage, Int].as(Learn(_), _.value)

  given BSONHandler[Practice] = typedMapHandler[StudyId, Int].as(Practice(_), _.value)

  given BSONDocumentHandler[Corres] = Macros.handler
  given BSONHandler[Patron] = BSONIntegerHandler.as(Patron.apply, _.months)

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

  given lila.db.BSON[SwissRank] with
    def reads(r: lila.db.BSON.Reader) = SwissRank(SwissId(r.str("i")), Rank(r.intD("r")))
    def writes(w: lila.db.BSON.Writer, s: SwissRank) = BSONDocument("i" -> s.id, "r" -> s.rank)

  object ActivityFields:
    val id = "_id"
    val games = "g"
    val forumPosts = "p"
    val ublogPosts = "u"
    val puzzles = "z"
    val storm = "m"
    val racer = "c"
    val streak = "k"
    val learn = "l"
    val practice = "r"
    val simuls = "s"
    val corres = "o"
    val patron = "a"
    val follows = "f"
    val studies = "t"
    val teams = "e"
    val swisses = "w"
    val stream = "st"

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
      id -> o.id,
      games -> o.games,
      forumPosts -> o.forumPosts,
      ublogPosts -> o.ublogPosts,
      puzzles -> o.puzzles,
      storm -> o.storm,
      racer -> o.racer,
      streak -> o.streak,
      learn -> o.learn,
      practice -> o.practice,
      simuls -> o.simuls,
      corres -> o.corres,
      patron -> o.patron,
      follows -> o.follows,
      studies -> o.studies,
      teams -> o.teams,
      swisses -> o.swisses,
      stream -> o.stream.option(true)
    )
