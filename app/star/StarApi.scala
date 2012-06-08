package lila
package star

import game.DbGame
import user.{ User, UserRepo }

import scalaz.effects._

final class StarApi(
    starRepo: StarRepo,
    userRepo: UserRepo,
    paginator: PaginatorBuilder) {

  def starred(game: DbGame, user: User): IO[Boolean] =
    starRepo.exists(game.id, user.id)

  def countByUser(user: User): IO[Int] =
    starRepo countByUserId user.id

  def usersByGame(game: DbGame): IO[List[User]] = for {
    userIds ← starRepo userIdsByGameId game.id
    users ← (userIds map userRepo.byId).sequence
  } yield users.flatten

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user: User, page: Int) map (_.game)
}
