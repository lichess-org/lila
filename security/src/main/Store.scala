package lila.security

import lila.user.User
import lila.db.Implicits._
import lila.db.DbApi

import play.api.mvc.RequestHeader
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import play.modules.reactivemongo.Implicits._
import org.joda.time.DateTime
import scala.concurrent.Future

case class UserSpy(
  ips: List[String],
  uas: List[String],
  otherUsernames: Set[String])

private[security] final class Store(coll: ReactiveColl) extends DbApi {

  def save(sessionId: String, username: String, req: RequestHeader): Funit =
    coll.insert(Json.obj(
      "_id" -> sessionId,
      "user" -> normalize(username),
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now,
      "up" -> true)).void

  def getUsername(sessionId: String): Fu[Option[String]] =
    query(select(sessionId))
      .projection(Json.obj("user" -> true, "up" -> true))
      .one map { objOption ⇒
        objOption flatMap { obj ⇒
          obj.get[String]("user") filter (_ ⇒ ~(obj.get[Boolean]("up")))
        }
      }

  def delete(sessionId: String): Funit =
    coll.update(select(sessionId), $set("up" -> false)).void

  // useful when closing an account,
  // we want to logout too
  def deleteUsername(username: String): Funit = coll.update(
    Json.obj("user" -> normalize(username)),
    $set("up" -> false),
    upsert = false,
    multi = true).void

  def userSpy(username: String): Fu[UserSpy] = for {
    objs ← query(Json.obj("user" -> normalize(username))).cursor.toList
    usernames ← explore(normalize(username))
  } yield UserSpy(
    ips = objs.map(_.get[String]("ip")).flatten.distinct,
    uas = objs.map(_.get[String]("ua")).flatten.distinct,
    otherUsernames = usernames
  )

  private def explore(username: String, withKnown: Set[String] = Set.empty): Fu[Set[String]] = {
    val known = withKnown + username
    newSiblings(username, known) flatMap { children ⇒
      children.foldLeft(fuccess(children)) {
        case (siblings, child) ⇒ siblings flatMap { sibs ⇒
          explore(child, known ++ sibs) map (sibs ++)
        }
      }
    }
  }

  private def newSiblings(username: String, without: Set[String]): Fu[Set[String]] =
    userIps(username) flatMap { ips ⇒
      Future.traverse(ips)(usernamesByIp) map (_.flatten diff without)
    }

  private def userIps(username: String): Fu[Set[String]] =
    query(Json.obj("user" -> normalize(username)))
      .projection(Json.obj("ip" -> true))
      .cursor.toList map { objs ⇒
        objs.map((obj: JsObject) ⇒ obj.get[String]("ip")).flatten.toSet
      }

  private def usernamesByIp(ip: String): Fu[Set[String]] =
    query(Json.obj("ip" -> ip))
      .projection(Json.obj("user" -> true))
      .cursor.toList map { objs ⇒
        objs.map((obj: JsObject) ⇒ obj.get[String]("user")).flatten.toSet
      }

  private def query(selector: JsObject) = coll.genericQueryBuilder query selector

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def normalize(username: String) = username.toLowerCase
}
