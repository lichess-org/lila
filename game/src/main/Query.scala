package lila.game

import chess.{ Color, Status }
import lila.db.DbApi

import play.api.libs.json._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

object Query extends DbApi {

  val all: JsObject = select.all

  val rated: JsObject = Json.obj("ra" -> true)

  def rated(u: String): JsObject = user(u) ++ rated

  val started: JsObject = Json.obj("s" -> $gte(Status.Started.id))

  def started(u: String): JsObject = user(u) ++ started

  val playable = Json.obj("s" -> $lt(Status.Aborted.id))

  val mate = Json.obj("s" -> Status.Mate.id)

  val draw: JsObject = Json.obj("s" -> $in(Status.Draw.id, Status.Stalemate.id))

  def draw(u: String): JsObject = user(u) ++ draw

  val finished = Json.obj("s" -> $in(Status.Mate.id, Status.Resign.id, Status.Outoftime.id, Status.Timeout.id))

  val notFinished: JsObject = Json.obj("s" -> $lte(Status.Started.id))

  def notFinished(u: String): JsObject = user(u) ++ notFinished

  val frozen = Json.obj("s" -> $gte(Status.Mate.id))

  val popular = Json.obj("bm" -> $gt(0))

  val imported = Json.obj("me.so" -> Source.Import.id)

  def pgnImport(pgn: String) = imported ++ Json.obj("me.pgni.pgn" -> pgn)

  def clock(c: Boolean) = Json.obj("c" -> $exists(c))

  def user(u: String) = Json.obj("uids" -> u)

  // use the uids index
  def win(u: String) = user(u) ++ Json.obj("wid" -> u)

  def loss(u: String) = user(u) ++ finished ++ Json.obj("wid" -> $ne(u))

  // TODO the sort does not belong here
  // def opponents(u1: String, u2: String) = Json.obj("uids" -> $all(u1, u2)).sortBy(_.nbGames).map(_.id)

  def turnsGt(nb: Int) = Json.obj("t" -> $gt(nb))

  val sortCreated = sort desc "ca" 

  val sortPopular = sort desc "bm" 
}
