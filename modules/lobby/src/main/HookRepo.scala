package lila.lobby

import tube.hookTube
import lila.game.Game
import lila.db.Implicits._
import lila.db.api._

import play.api.libs.json._

import reactivemongo.api._
import reactivemongo.bson._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

object HookRepo {

  def ownedHook(ownerId: String): Fu[Option[Hook]] =
    $find.one(Json.obj("ownerId" -> ownerId))

  def allOpen = hookList(Json.obj("match" -> false))

  def allOpenCasual = hookList(Json.obj("match" -> false, "mode" -> 0))

  def hookList(selector: JsObject): Fu[List[Hook]] =
    $find($query(selector) sort $sort.createdAsc)

  def setGame(hook: Hook, game: Game) = $update(
    $select(hook.id),
    $set(Json.obj("match" -> true, "gameId" -> game.id)))

  def removeOwnerId(ownerId: String): Funit =
    $remove(Json.obj("ownerId" -> ownerId))

  def unmatchedNotInOwnerIds(ids: Iterable[String]): Fu[List[Hook]] =
    $find(Json.obj("ownerId" -> $nin(ids), "match" -> false))

  def cleanupOld: Fu[Unit] =
    $remove(Json.obj("createdAt" -> $lt(DateTime.now - 1.hour)))
}
