package lila.activity

import reactivemongo.bson._

import lila.common.Iso
import lila.db.BSON.{ MapDocument, MapValue }
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeKeyIso
import lila.rating.PerfType

private object BSONHandlers {

  import Activity._

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
  private implicit val ratingDiffHandler = intAnyValHandler[RatingDiff](_.value, RatingDiff.apply)

  private implicit val scoreHandler = new lila.db.BSON[Score] {
    private val win = "w"
    private val loss = "l"
    private val draw = "d"
    private val rd = "r"

    def reads(r: lila.db.BSON.Reader) = Score(
      win = r.intD(win),
      loss = r.intD(loss),
      draw = r.intD(draw),
      rd = r.getD[RatingDiff](rd)
    )

    def writes(w: lila.db.BSON.Writer, o: Score) = BSONDocument(
      win -> w.intO(o.win),
      loss -> w.intO(o.loss),
      draw -> w.intO(o.draw),
      rd -> w.zero(o.rd)
    )
  }

  private implicit val gamesMapHandler = MapDocument.MapHandler[PerfType, Score]
  private implicit val gamesHandler = isoHandler[Games, Map[PerfType, Score], Bdoc]((g: Games) => g.value, Games.apply _)

  private implicit val gameIdHandler = stringAnyValHandler[GameId](_.value, GameId.apply)
  private implicit val gameIdsHandler = bsonArrayToListHandler[GameId]
  private implicit val compAnalysisHandler = isoHandler[CompAnalysis, List[GameId], Barr]((c: CompAnalysis) => c.gameIds, CompAnalysis.apply _)

  private implicit val topicIdIso = Iso.string[Posts.TopicId](Posts.TopicId.apply, _.value)
  private implicit val postIdHandler = stringAnyValHandler[Posts.PostId](_.value, Posts.PostId.apply)
  private implicit val postIdsHandler = bsonArrayToListHandler[Posts.PostId]
  private implicit val postsMapHandler = MapValue.MapHandler[Posts.TopicId, List[Posts.PostId]]
  private implicit val postsHandler = isoHandler[Posts, Map[Posts.TopicId, List[Posts.PostId]], Bdoc]((p: Posts) => p.posts, Posts.apply _)

  private implicit val puzzleIdHandler = intAnyValHandler[PuzzleId](_.value, PuzzleId.apply)
  private implicit val puzzleListHandler = Macros.handler[PuzzleList]
  private implicit val puzzlesHandler = new lila.db.BSON[Puzzles] {

    def reads(r: lila.db.BSON.Reader) = Puzzles(
      win = r.getD[PuzzleList]("w"),
      loss = r.getD[PuzzleList]("l"),
      ratingProg = r.getO[RatingProg]("r")
    )

    def writes(w: lila.db.BSON.Writer, o: Puzzles) = BSONDocument(
      "w" -> w.zero(o.win),
      "l" -> w.zero(o.loss),
      "r" -> o.ratingProg
    )
  }

  implicit val activityHandler = new lila.db.BSON[Activity] {

    private val id = "_id"
    private val games = "g"
    private val comps = "c"
    private val posts = "p"
    private val puzzles = "z"

    def reads(r: lila.db.BSON.Reader) = Activity(
      id = r.get[Id](id),
      games = r.getD[Games](games),
      comps = r.getD[CompAnalysis](comps),
      posts = r.getD[Posts](posts),
      puzzles = r.getD[Puzzles](puzzles)
    )

    def writes(w: lila.db.BSON.Writer, o: Activity) = BSONDocument(
      id -> o.id,
      games -> w.zero(o.games),
      comps -> w.zero(o.comps),
      posts -> w.zero(o.posts),
      puzzles -> w.zero(o.puzzles)
    )
  }
}
