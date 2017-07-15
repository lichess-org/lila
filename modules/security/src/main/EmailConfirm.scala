package lila.security

import lila.common.EmailAddress
import lila.user.{ User, UserRepo }

trait EmailConfirm {

  def effective: Boolean

  def send(user: User, email: EmailAddress): Funit

  def confirm(token: String): Fu[Option[User]]
}

object EmailConfirmSkip extends EmailConfirm {

  def effective = false

  def send(user: User, email: EmailAddress) = UserRepo setEmailConfirmed user.id

  def confirm(token: String): Fu[Option[User]] = fuccess(none)
}

final class EmailConfirmMailgun(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) extends EmailConfirm {

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress): Funit = tokener make user.id flatMap { token =>
    lila.mon.email.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    mailgun send Mailgun.Message(
      to = email,
      subject = s"Confirm your lichess.org account, ${user.username}",
      text = s"""
Final step!

Confirm your email address to complete your lichess account. It's easy — just click on the link below.

$url

(Clicking not working? Try pasting it into your browser!)

This is a service email related to your use of lichess.org. If you did not register with Lichess you can safely ignore this message.""",
      htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <strong>Final step!</strong>
  <p itemprop="description">Confirm your email address to activate your Lichess account. It's easy — just click the link below.</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Activate account">
    ${Mailgun.html.url(url)}
  </div>
  ${Mailgun.html.serviceNote}
</div>""".some
    )
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token flatMap {
    _ ?? { userId =>
      UserRepo.mustConfirmEmail(userId) flatMap {
        _ ?? {
          (UserRepo setEmailConfirmed userId) >> (UserRepo byId userId)
        }
      }
    }
  }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo email id map (_.??(_.value))
  )
}
