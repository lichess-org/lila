package lila.security

import org.joda.time.DateTime

import lila.user.{ User, UserRepo }

final class LoginToken(secret: String) {

  def generate(user: User): Fu[String] = tokener make user

  def consume(token: String): Fu[Option[User]] = tokener read token

  private object DateStr {
    def toStr(date: DateTime) = date.getMillis.toString
    def toDate(str: String) = parseLongOption(str) map { new DateTime(_) }
  }

  private val tokener = new UserTokener(
    secret = secret,
    getCurrentValue = id => fuccess(DateStr toStr DateTime.now),
    currentValueHashSize = none,
    valueChecker = UserTokener.ValueChecker.Custom(v => fuccess {
      DateStr.toDate(v) exists DateTime.now.minusMinutes(1).isBefore
    })
  )
}
