package lila.timeline

import lila.db.{ Tube, CappedRepo }
import lila.db.Types._

import reactivemongo.bson._

final class EntryRepo(val max: Int, val json: Tube[Entry])(implicit val coll: Coll)
    extends CappedRepo[Entry]
    with lila.db.api.Full {

  def add(entry: Entry): Funit = insert(json toMongo entry).void

  // def decode(obj: DBObject): Option[Entry] = for {
  //   gameId ← obj.getAs[String]("gameId")
  //   whiteName ← obj.getAs[String]("whiteName")
  //   blackName ← obj.getAs[String]("blackName")
  //   whiteId = obj.getAs[String]("whiteId")
  //   blackId = obj.getAs[String]("blackId")
  //   variant ← obj.getAs[String]("variant")
  //   rated ← obj.getAs[Boolean]("rated")
  //   clock = obj.getAs[String]("clock")
  // } yield Entry(gameId, whiteName, blackName, whiteId, blackId, variant, rated, clock)

  // def encode(obj: Entry): DBObject = DBObject(
  //   "gameId" -> obj.gameId,
  //   "whiteName" -> obj.whiteName,
  //   "blackName" -> obj.blackName,
  //   "whiteId" -> obj.whiteId,
  //   "blackId" -> obj.blackId,
  //   "variant" -> obj.variant,
  //   "rated" -> obj.rated,
  //   "clock" -> obj.clock)
}
