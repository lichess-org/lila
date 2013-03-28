package lila.security

import lila.common.PimpedJson._
import lila.user.User
import lila.db.Types.Coll

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

private[security] final class Store(implicit coll: Coll) extends lila.db.api.Full {

  def save(sessionId: String, username: String, req: RequestHeader): Funit =
    coll.insert(Json.obj(
      "_id" -> sessionId,
      "user" -> normalize(username),
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now,
      "up" -> true)).void

  def getUsername(sessionId: String): Fu[Option[String]] =
    primitive.one(
      select(sessionId) ++ Json.obj("up" -> true), 
      "user"
    )(_.asOpt[String])

  def delete(sessionId: String): Funit =
    coll.update(select(sessionId), $set("up" -> false)).void

  // useful when closing an account,
  // we want to logout too
  def deleteUsername(username: String): Funit = coll.update(
    selectUser(username),
    $set("up" -> false),
    upsert = false,
    multi = true).void

  def userSpy(username: String): Fu[UserSpy] = for {
    objs ← builder.query(selectUser(username)).cursor.toList
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
    primitive(selectUser(username), "ip")(_.asOpt[String]) map (_.toSet)

  private def usernamesByIp(ip: String): Fu[Set[String]] =
    primitive(Json.obj("ip" -> ip), "user")(_.asOpt[String]) map (_.toSet)

  protected implicit val builder = coll.genericQueryBuilder

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def normalize(username: String) = username.toLowerCase

  private def selectUser(username: String) = Json.obj("user" -> normalize(username))
}
