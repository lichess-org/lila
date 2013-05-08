package lila.security

import lila.common.PimpedJson._
import lila.user.User
import lila.db.Types.Coll
import lila.db.api._
import tube.storeTube

import play.api.mvc.RequestHeader
import play.api.libs.json._

import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import org.joda.time.DateTime
import scala.concurrent.Future

case class UserSpy(
  ips: List[String],
  uas: List[String],
  otherUsernames: Set[String])

object Store {

  def save(sessionId: String, user: String, req: RequestHeader): Funit =
    $insert(Json.obj(
      "_id" -> sessionId,
      "user" -> normalize(user),
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now,
      "up" -> true))

  def getUsername(sessionId: String): Fu[Option[String]] =
    $primitive.one(
      $select(sessionId) ++ Json.obj("up" -> true), 
      "user"
    )(_.asOpt[String])

  def delete(sessionId: String): Funit =
    $update($select(sessionId), $set("up" -> false))

  // useful when closing an account,
  // we want to logout too
  def deleteUsername(user: String): Funit = $update(
    selectUser(user),
    $set("up" -> false),
    upsert = false,
    multi = true)

  private[security] def userSpy(user: String): Fu[UserSpy] = for {
    objs ← $find(selectUser(user))
    usernames ← explore(normalize(user))
  } yield UserSpy(
    ips = objs.map(_ str "ip").flatten.distinct,
    uas = objs.map(_ str "ua").flatten.distinct,
    otherUsernames = usernames
  )

  private def explore(user: String, withKnown: Set[String] = Set.empty): Fu[Set[String]] = {
    val known = withKnown + user
    newSiblings(user, known) flatMap { children ⇒
      children.foldLeft(fuccess(children)) {
        case (siblings, child) ⇒ siblings flatMap { sibs ⇒
          explore(child, known ++ sibs) map (sibs ++)
        }
      }
    }
  }

  private def newSiblings(user: String, without: Set[String]): Fu[Set[String]] =
    userIps(user) flatMap { ips ⇒
      Future.traverse(ips)(usernamesByIp) map (_.flatten diff without)
    }

  private def userIps(user: String): Fu[Set[String]] =
    $primitive(selectUser(user), "ip")(_.asOpt[String]) map (_.toSet)

  private def usernamesByIp(ip: String): Fu[Set[String]] =
    $primitive(Json.obj("ip" -> ip), "user")(_.asOpt[String]) map (_.toSet)

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def normalize(username: String) = username.toLowerCase

  private def selectUser(username: String) = Json.obj("user" -> normalize(username))
}
