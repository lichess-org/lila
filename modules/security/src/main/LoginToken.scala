package lila.security

import lila.core.config.Secret
import lila.user.{ User, UserRepo }

final class LoginToken(secret: Secret, userRepo: UserRepo)(using Executor):

  def generate(user: User): Fu[String] = tokener.make(user.id)

  def consume(token: String): Fu[Option[User]] =
    tokener.read(token).flatMapz(userRepo.byId)

  private val tokener = LoginToken.makeTokener(secret, 1 minute)

private object LoginToken:

  import StringToken.DateStr

  def makeTokener(secret: Secret, lifetime: FiniteDuration)(using Executor) =
    new StringToken[UserId](
      secret = secret,
      getCurrentValue = _ => fuccess(DateStr.toStr(nowInstant)),
      currentValueHashSize = none,
      valueChecker = StringToken.ValueChecker.Custom(v =>
        fuccess {
          DateStr.toInstant(v).exists(nowInstant.minus(lifetime).isBefore)
        }
      )
    )
