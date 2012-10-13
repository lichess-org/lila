package lila
package game

import chess.{ Color, Status }
import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

object Query {
  
  val all = DBObject()

  val rated: DBObject = DBObject("ra" -> true)

  def rated(u: User): DBObject = user(u) ++ rated

  val started: DBObject = "s" $gte Status.Started.id

  def started(u: User): DBObject = user(u) ++ started

  val playable = "s" $lt Status.Aborted.id

  val mate = DBObject("s" -> Status.Mate.id)

  val draw: DBObject = "s" $in List(Status.Draw.id, Status.Stalemate.id)

  def draw(u: User): DBObject = user(u) ++ draw

  val finished = "s" $in List(Status.Mate.id, Status.Resign.id, Status.Outoftime.id, Status.Timeout.id)

  val notFinished: DBObject = "s" $lte Status.Started.id

  val frozen = "s" $gte Status.Mate.id

  val popular = "bm" $gt 0

  def clock(c: Boolean) = "c" $exists c

  def user(u: User) = DBObject("uids" -> u.id)

  // use the uids index
  def win(u: User) = user(u) ++ ("wid" -> u.id)

  def loss(u: User) = user(u) ++ finished ++ ("wid" $ne u.id)

  def notFinished(u: User): DBObject = user(u) ++ notFinished

  def opponents(u1: User, u2: User) = "uids" $all List(u1.id, u2.id)

  def turnsGt(nb: Int) = "t" $gt nb

  val sortCreated = DBObject("ca" -> -1)

  val sortPopular = DBObject("bm" -> -1)
}
