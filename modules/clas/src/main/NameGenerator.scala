package lila.clas

import lila.common.CuteNameGenerator

final class NameGenerator(userRepo: lila.user.UserRepo)(using Executor):

  def apply(maxSize: Int = 17, triesLeft: Int = 20): Fu[Option[UserName]] =
    CuteNameGenerator.make(maxSize) so { name =>
      userRepo.exists(name) flatMap {
        case true if triesLeft > 0 => apply(maxSize, triesLeft - 1)
        case true                  => fuccess(none)
        case _                     => fuccess(name.some)
      }
    }
