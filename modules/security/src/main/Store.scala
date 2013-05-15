package lila.security

import lila.common.PimpedJson._
import lila.user.{ User, UserRepo }
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
  otherUsers: Set[User])

object Store {

  def save(sessionId: String, userId: String, req: RequestHeader): Funit =
    $insert(Json.obj(
      "_id" -> sessionId,
      "user" -> userId,
      "ip" -> ip(req),
      "ua" -> ua(req),
      "date" -> DateTime.now,
      "up" -> true))

  def userId(sessionId: String): Fu[Option[String]] =
    $primitive.one(
      $select(sessionId) ++ Json.obj("up" -> true),
      "user"
    )(_.asOpt[String])

  def delete(sessionId: String): Funit =
    $update($select(sessionId), $set("up" -> false))

  // useful when closing an account,
  // we want to logout too
  def deleteUser(userId: String): Funit = $update(
    selectUser(userId),
    $set("up" -> false),
    upsert = false,
    multi = true)

  private[security] def userSpy(userId: String): Fu[UserSpy] = for {
    user ← UserRepo byId userId flatten "[spy] user not found"
    objs ← $find(selectUser(user.id))
    users ← explore(user)
  } yield UserSpy(
    ips = objs.map(_ str "ip").flatten.distinct,
    uas = objs.map(_ str "ua").flatten.distinct,
    otherUsers = users
  )

  private def explore(user: User, withKnown: Set[User] = Set.empty): Fu[Set[User]] = {
    val known = Seq(user) ++: withKnown
    newSiblings(user.id, known) flatMap { children ⇒
      children.foldLeft(fuccess(children)) {
        case (siblings, child) ⇒ siblings flatMap { sibs ⇒
          explore(child, known ++ sibs) map (sibs ++)
        }
      }
    }
  }

  private def newSiblings(user: String, without: Set[User]): Fu[Set[User]] =
    userIps(user) flatMap { ips ⇒
      usersByIps(ips) map (_ diff without)
    }

  private def userIps(user: String): Fu[Set[String]] =
    $primitive(selectUser(user), "ip")(_.asOpt[String]) map (_.toSet)

  private def usersByIps(ips: Set[String]): Fu[Set[User]] =
    $primitive(
      Json.obj("ip" -> $in(ips)), "user"
    )(_.asOpt[String]) flatMap UserRepo.byIds map (_.toSet)

  private def ip(req: RequestHeader) = req.remoteAddress

  private def ua(req: RequestHeader) = req.headers.get("User-Agent") | "?"

  private def selectUser(userId: String) = Json.obj("user" -> userId)
}
