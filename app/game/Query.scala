package lila
package game

import chess.{ Color, Variant, Status }
import user.User

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

object Query {
  
  val all = DBObject()

  val rated: DBObject = DBObject("isRated" -> true)

  val started: DBObject = ("status" $gte Status.Started.id)

  val playable: DBObject = ("status" $lt Status.Aborted.id)

  val mate: DBObject = DBObject("status" -> Status.Mate.id)

  val draw: DBObject = "status" $in List(Status.Draw.id, Status.Stalemate.id)

  val finished: DBObject = "status" $in List(Status.Mate.id, Status.Resign.id, Status.Outoftime.id, Status.Timeout.id)

  val notFinished: DBObject = DBObject("status" -> Status.Started.id)

  val popular: DBObject = "bm" $gt 0

  def clock(c: Boolean): DBObject = "clock.l" $exists c

  def user(u: User): DBObject = DBObject("userIds" -> u.id)

  def started(u: User): DBObject = user(u) ++ started

  def rated(u: User): DBObject = user(u) ++ rated

  def win(u: User): DBObject = DBObject("winId" -> u.id)

  def draw(u: User): DBObject = user(u) ++ draw

  def loss(u: User): DBObject = user(u) ++ finished ++ ("winId" $ne u.id)

  def notFinished(u: User): DBObject = user(u) ++ notFinished

  def opponents(u1: User, u2: User) = "userIds" $all List(u1.id, u2.id)

  def turnsGt(nb: Int) = "turns" $gt nb

  val sortCreated = DBObject("createdAt" -> -1)

  val sortPopular = DBObject("bm" -> -1)
}
