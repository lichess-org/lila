package lila.friend

import lila.game.GameRepo
import lila.user.{ User, UserRepo }
import lila.user.tube.userTube
import tube.{ friendTube, requestTube }
import lila.db.api._
import lila.db.Implicits._

import org.scala_tools.time.Imports._

final class FriendApi(cached: Cached) {

  def areFriends(u1: String, u2: String): Fu[Boolean] =
    cached friendIds u1 map (_ contains u2)

  def requestable(userId: String, friendId: String): Fu[Boolean] =
    RequestRepo.exists(userId, friendId) map (!_)

  def createRequest(friend: User, setup: RequestSetup, user: User): Funit =
    requestable(friend.id, user.id) flatMap {
      _ ?? $insert(Request.make(user = user.id, friend = friend.id, message = setup.message)) >>
        (cached.nbRequests remove friend.id)
    }

  def requestsWithUsers(userId: String): Fu[List[RequestWithUser]] = for {
    requests ← RequestRepo findByFriendId userId
    users ← $find.byOrderedIds[User](requests.map(_.user))
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def processRequest(userId: String, request: Request, accept: Boolean): Funit =
    $remove.byId[Request](request.id) >>
      (cached.nbRequests remove userId) >>
      accept ?? $find.byId[User](request.user) flatten "requester not found" flatMap { requester ⇒
        makeFriends(requester.id, userId)
      }

  private[friend] def makeFriends(u1: String, u2: String): Funit =
    FriendRepo.add(u1, u2) >>
      (cached.friendIds remove u1) >>
      (cached.friendIds remove u2)

  def friendsOf(userId: String): Fu[List[User]] =
    cached friendIds userId flatMap UserRepo.byIds map { _ sortBy (_.id) }
}
