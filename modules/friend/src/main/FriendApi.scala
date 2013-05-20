package lila.friend

import lila.game.GameRepo
import lila.user.{ User, UserRepo }
import lila.user.tube.userTube
import tube.{ friendTube, requestTube }
import lila.db.api._
import lila.db.Implicits._

import org.scala_tools.time.Imports._

final class FriendApi(cached: Cached) {

  private type ID = String

  def areFriends(u1: ID, u2: ID): Fu[Boolean] =
    cached friendIds u1 map (_ contains u2)

  def requestable(userId: ID, friendId: ID): Fu[Boolean] =
    !areFriends(userId, friendId) >>& !RequestRepo.exists(userId, friendId)

  def createRequest(friendId: ID, userId: ID): Fu[Status] =
    requestable(friendId, userId) flatMap {
      _.fold({
        val req = Request.make(user = userId, friend = friendId)
        $insert(req) >> (cached.nbRequests remove friendId) inject Status(req)
      },
        fufail("[friend] cannot create request")
      )
    }

  def requestsWithUsers(userId: ID): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo findByFriendId userId
    users ← $find.byOrderedIds[User](requests.map(_.user))
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def processRequest(userId: ID, request: Request, accept: Boolean): Funit =
    $remove.byId[Request](request.id) >>
      (cached.nbRequests remove userId) >>
      accept ?? $find.byId[User](request.user) flatten "requester not found" flatMap { requester ⇒
        makeFriends(requester.id, userId)
      }

  private[friend] def makeFriends(u1: ID, u2: ID): Funit =
    FriendRepo.add(u1, u2) >>
      (cached.friendIds remove u1) >>
      (cached.friendIds remove u2)

  def friendsOf(userId: ID): Fu[List[User]] =
    cached friendIds userId flatMap UserRepo.byIds map { _ sortBy (_.id) }
}
