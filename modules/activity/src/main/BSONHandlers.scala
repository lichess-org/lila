package lila.activity

import reactivemongo.bson._

import lila.common.Iso
import lila.db.dsl._
import lila.db.BSON.{ MapDocument, MapValue }
import lila.rating.PerfType
import lila.rating.BSONHandlers.perfTypeKeyIso

private object BSONHandlers {

  import Activity._

  private implicit val dayHandler = intAnyValHandler[DaySinceSignup](_.value, DaySinceSignup.apply)
  private implicit val scoreHandler = Macros.handler[Score]

  private implicit val gamesMapHandler = MapDocument.MapHandler[PerfType, Score]
  private implicit val gamesHandler = isoHandler[Games, Map[PerfType, Score], Bdoc]((g: Games) => g.value, Games.apply _)

  private implicit val tourIdIso = Iso.string[Tours.TourId](Tours.TourId.apply, _.value)
  private implicit val tourResultHandler = {
    import Tours.Result
    Macros.handler[Result]
  }
  private implicit val toursMapHandler = MapDocument.MapHandler[Tours.TourId, Tours.Result]
  private implicit val toursHandler = isoHandler[Tours, Map[Tours.TourId, Tours.Result], Bdoc]((t: Tours) => t.value, Tours.apply _)

  private implicit val gameIdHandler = stringAnyValHandler[GameId](_.value, GameId.apply)
  private implicit val gameIdsHandler = bsonArrayToListHandler[GameId]
  private implicit val compAnalysisHandler = isoHandler[CompAnalysis, List[GameId], Barr]((c: CompAnalysis) => c.gameIds, CompAnalysis.apply _)

  private implicit val threadIdIso = Iso.string[Posts.ThreadId](Posts.ThreadId.apply, _.value)
  private implicit val postIdHandler = stringAnyValHandler[Posts.PostId](_.value, Posts.PostId.apply)
  private implicit val postIdsHandler = bsonArrayToListHandler[Posts.PostId]
  private implicit val postsMapHandler = MapValue.MapHandler[Posts.ThreadId, List[Posts.PostId]]
  private implicit val postsHandler = isoHandler[Posts, Map[Posts.ThreadId, List[Posts.PostId]], Bdoc]((p: Posts) => p.posts, Posts.apply _)

  implicit val activityHandler = Macros.handler[Activity]
}
