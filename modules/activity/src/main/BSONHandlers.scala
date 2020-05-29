package lidraughts.activity

import reactivemongo.bson._

import lidraughts.common.Iso
import lidraughts.db.BSON.{ MapDocument, MapValue }
import lidraughts.db.dsl._
import lidraughts.rating.BSONHandlers.perfTypeKeyIso
import lidraughts.rating.PerfType
import lidraughts.study.Study
import lidraughts.user.User

private object BSONHandlers {

  import Activity._
  import activities._
  import model._

  def regexId(userId: User.ID): Bdoc = "_id" $startsWith s"$userId:"

  implicit val activityIdHandler: BSONHandler[BSONString, Id] = new BSONHandler[BSONString, Id] {
    private val sep = ':'
    def read(bs: BSONString) = bs.value split sep match {
      case Array(userId, dayStr) => Id(userId, Day(Integer.parseInt(dayStr)))
      case _ => sys error s"Invalid activity id ${bs.value}"
    }
    def write(id: Id) = BSONString(s"${id.userId}$sep${id.day.value}")
  }

  private implicit val ratingHandler = intAnyValHandler[Rating](_.value, Rating.apply)
  private implicit val ratingProgHandler: BSONHandler[Barr, RatingProg] = new BSONHandler[Barr, RatingProg] {
    def read(b: BSONArray) = (for {
      before <- b.getAs[Rating](0)
      after <- b.getAs[Rating](1)
    } yield RatingProg(before, after)) err s"Invalid rating prog ${b.elements}"
    def write(o: RatingProg) = BSONArray(o.before, o.after)
  }

  private implicit val scoreHandler = new lidraughts.db.BSON[Score] {
    private val win = "w"
    private val loss = "l"
    private val draw = "d"
    private val rp = "r"

    def reads(r: lidraughts.db.BSON.Reader) = Score(
      win = r.intD(win),
      loss = r.intD(loss),
      draw = r.intD(draw),
      rp = r.getO[RatingProg](rp)
    )

    def writes(w: lidraughts.db.BSON.Writer, o: Score) = BSONDocument(
      win -> w.intO(o.win),
      loss -> w.intO(o.loss),
      draw -> w.intO(o.draw),
      rp -> o.rp
    )
  }

  private implicit val gamesMapHandler = MapDocument.MapHandler[PerfType, Score]
  implicit val gamesHandler = isoHandler[Games, Map[PerfType, Score], Bdoc]((g: Games) => g.value, Games.apply _)

  private implicit val gameIdHandler = stringAnyValHandler[GameId](_.value, GameId.apply)
  private implicit val gameIdsHandler = bsonArrayToListHandler[GameId]

  private implicit val postIdHandler = stringAnyValHandler[PostId](_.value, PostId.apply)
  private implicit val postIdsHandler = bsonArrayToListHandler[PostId]
  implicit val postsHandler = isoHandler[Posts, List[PostId], Barr]((p: Posts) => p.value, Posts.apply _)

  implicit val puzzlesHandler = isoHandler[Puzzles, Score, Bdoc]((p: Puzzles) => p.score, Puzzles.apply _)

  private implicit val learnStageIso = Iso.string[Learn.Stage](Learn.Stage.apply, _.value)
  private implicit val learnMapHandler = MapValue.MapHandler[Learn.Stage, Int]
  private implicit val learnHandler = isoHandler[Learn, Map[Learn.Stage, Int], Bdoc]((l: Learn) => l.value, Learn.apply _)

  private implicit val studyIdIso = Iso.string[Study.Id](Study.Id.apply, _.value)
  private implicit val practiceMapHandler = MapValue.MapHandler[Study.Id, Int]
  private implicit val practiceHandler = isoHandler[Practice, Map[Study.Id, Int], Bdoc]((p: Practice) => p.value, Practice.apply _)

  private implicit val simulIdHandler = stringAnyValHandler[SimulId](_.value, SimulId.apply)
  private implicit val simulIdsHandler = bsonArrayToListHandler[SimulId]
  private implicit val simulsHandler = isoHandler[Simuls, List[SimulId], Barr]((s: Simuls) => s.value, Simuls.apply _)

  implicit val corresHandler = Macros.handler[Corres]
  private implicit val patronHandler = intAnyValHandler[Patron](_.months, Patron.apply)

  private implicit val followIdsHandler = bsonArrayToListHandler[User.ID]
  private implicit val followListHandler = Macros.handler[FollowList]

  private implicit val followsHandler = new lidraughts.db.BSON[Follows] {
    def reads(r: lidraughts.db.BSON.Reader) = Follows(
      in = r.getO[FollowList]("i").filterNot(_.isEmpty),
      out = r.getO[FollowList]("o").filterNot(_.isEmpty)
    )
    def writes(w: lidraughts.db.BSON.Writer, o: Follows) = BSONDocument(
      "i" -> o.in,
      "o" -> o.out
    )
  }

  private implicit val studyIdHandler = isoHandler(studyIdIso)
  private implicit val studyIdsHandler = bsonArrayToListHandler[Study.Id]
  private implicit val studiesHandler = isoHandler[Studies, List[Study.Id], Barr]((s: Studies) => s.value, Studies.apply _)
  private implicit val teamsHandler = isoHandler[Teams, List[String], Barr]((s: Teams) => s.value, Teams.apply _)

  object ActivityFields {
    val id = "_id"
    val games = "g"
    val posts = "p"
    val puzzles = "z"
    val puzzlesFrisian = "zf"
    val puzzlesRussian = "zr"
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

  implicit val activityHandler = new lidraughts.db.BSON[Activity] {

    import ActivityFields._

    def reads(r: lidraughts.db.BSON.Reader) = Activity(
      id = r.get[Id](id),
      games = r.getO[Games](games),
      posts = r.getO[Posts](posts),
      puzzles = r.getO[Puzzles](puzzles),
      puzzlesFrisian = r.getO[Puzzles](puzzlesFrisian),
      puzzlesRussian = r.getO[Puzzles](puzzlesRussian),
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

    def writes(w: lidraughts.db.BSON.Writer, o: Activity) = BSONDocument(
      id -> o.id,
      games -> o.games,
      posts -> o.posts,
      puzzles -> o.puzzles,
      puzzlesFrisian -> o.puzzlesFrisian,
      puzzlesRussian -> o.puzzlesRussian,
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
