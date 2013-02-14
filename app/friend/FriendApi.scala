package lila
package friend

import scalaz.effects._
import org.scala_tools.time.Imports._

import user.{ User, UserRepo }

final class FriendApi(
    friendRepo: FriendRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    cached: Cached) {

  def areFriends(u1: String, u2: String) = friendIds(u1) contains u2

  val friendIds = cached friendIds _

  def requestable(userId: String, friendId: String): IO[Boolean] =
    requestRepo.exists(userId, friendId) map (!_)

  def createRequest(friend: User, setup: RequestSetup, user: User): IO[Unit] = for {
    able ← requestable(friend.id, user.id)
    request = Request(user = user.id, friend = friend.id, message = setup.message)
    _ ← requestRepo.add(request) >> io(cached invalidateNbRequests friend.id) doIf able
  } yield ()

  def requestsWithUsers(userId: String): IO[List[RequestWithUser]] = for {
    requests ← requestRepo findByFriendId userId
    users ← userRepo byOrderedIds requests.map(_.user)
  } yield requests zip users map {
    case (request, user) ⇒ RequestWithUser(request, user)
  }

  def processRequest(userId: String, request: Request, accept: Boolean): IO[Unit] = for {
    _ ← requestRepo remove request.id
    _ ← io(cached invalidateNbRequests userId)
    requesterOption ← userRepo byId request.user
    _ ← ~requesterOption.map(requester ⇒
      friendRepo.add(requester.id, userId) >> 
      io(cached.invalidateFriendIds(requester.id)) >>
      io(cached.invalidateFriendIds(userId)) doIf accept
    )
  } yield ()

  def friendsOf(userId: String): IO[List[User]] =
    userRepo byIds friendIds(userId) map { _ sortBy (_.id) }
}
