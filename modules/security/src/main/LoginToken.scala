package lila.security

import org.joda.time.DateTime

import lila.user.{ User, UserRepo }

final class LoginToken(secret: String) {

  def generate(user: User): Fu[String] = tokener make user.id

  def consume(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? UserRepo.byId }

  private object DateStr {
    def toStr(date: DateTime) = date.getMillis.toString
    def toDate(str: String) = parseLongOption(str) map { new DateTime(_) }
  }

  //   implicit final val dateSerializable = new StringToken.Serializable[Option[DateTime]] {
  //     def read(str: String) = parseLongOption(str) map { new DateTime(_) }
  //     def write(date: Option[DateTime]) = date ?? (_.getMillis.toString)
  //   }

  private val tokener = new StringToken[User.ID](
    secret = secret,
    getCurrentValue = _ => fuccess(DateStr toStr DateTime.now),
    currentValueHashSize = none,
    valueChecker = StringToken.ValueChecker.Custom(v => fuccess {
      DateStr.toDate(v) exists DateTime.now.minusMinutes(1).isBefore
    })
  )
}
