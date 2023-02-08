package lila.clas

import scala.concurrent.ExecutionContext

import lila.common.CuteNameGenerator

final class NameGenerator(userRepo: lila.user.UserRepo)(using ExecutionContext):

  def apply(maxSize: Int = 17, triesLeft: Int = 20): Fu[Option[UserName]] =
    CuteNameGenerator.make(maxSize) ?? { name =>
      userRepo.exists(name) flatMap {
        case true if triesLeft > 0 => apply(maxSize, triesLeft - 1)
        case true                  => fuccess(none)
        case _                     => fuccess(name.some)
      }
    }
