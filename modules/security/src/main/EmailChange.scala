package lila.security

import lila.common.EmailAddress
import lila.user.{ User, UserRepo }

final class EmailChange(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) {

  def send(user: User, email: EmailAddress): Funit = tokener make user.id flatMap { token =>
    lila.mon.email.resetPassword()
    val url = s"$baseUrl/password/reset/confirm/$token"
    mailgun send Mailgun.Message(
      to = email,
      subject = s"Confirm new email address, ${user.username}",
      text = s"""
        You have requested to change your email address. To confirm you have access to this email, please click the link below:

        $url

        (Clicking not working? Try pasting it into your browser!)

        This message is a service email related to your use of lichess.org.""",
      htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">You have requested to change your email address.</p>
  <p>To confirm you have access to this email, please click the link below:</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Change email address">
    ${Mailgun.html.url(url)}
  </div>
  ${Mailgun.html.serviceNote}
</div>""".some
    )
  }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? UserRepo.byId }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo getPasswordHash id map (~_)
  )
}
