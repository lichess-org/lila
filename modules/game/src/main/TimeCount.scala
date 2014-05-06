package lila.game

import lila.db.api._
import org.joda.time.Period
import tube.gameTube

import reactivemongo.bson._

object TimeCount {

  def total(userId: String): Fu[Period] = fuccess(new Period(0))
  // gameTube.coll
  //   .find($query[BSONDocument](BSONDocument(Game.BSONFields.moveTimes -> userId)))
  //   .projection(BSONDocument("mt" -> true))
  //   .cursor[BSONDocument]
  //   .enumerate() |>>> (Iteratee.fold(0) {
  //     case (time, doc) => doc.getAs[ByteArray]("mt") map BinaryFormat.moveTimes.read ?? (_.sum)
  //   }) map { tenths => new Period(tenths.toLong * 100) }
}
