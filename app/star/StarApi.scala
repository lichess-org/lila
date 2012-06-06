package lila
package star

import game.{ DbGame, GameRepo }
import user.{ User, UserRepo }

import scalaz.effects._

final class StarApi(
  starRepo: StarRepo,
  gameRepo: GameRepo,
  userRepo: UserRepo) {

    def starred(game: DbGame, user: User): IO[Boolean] = 
      starRepo.exists(game.id, user.id)
}
