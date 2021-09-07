package lila.ublog

import scala.concurrent.ExecutionContext
import lila.user.UserRepo

final class UblogTier(userRepo: UserRepo)(implicit ec: ExecutionContext) {}
