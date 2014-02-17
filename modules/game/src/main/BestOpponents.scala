package lila.game

import lila.user.{ User, UserRepo }

object BestOpponents {

  def apply(userId: String, limit: Int): Fu[List[(User, Int)]] =
    GameRepo.bestOpponents(userId, limit) flatMap { opponents =>
      UserRepo enabledByIds opponents.map(_._1) map { users =>
        (users map { user =>
          opponents find (_._1 == user.id) map { opponent =>
            user -> opponent._2
          }
        }).flatten sortBy (-_._2)
      }
    }
}
