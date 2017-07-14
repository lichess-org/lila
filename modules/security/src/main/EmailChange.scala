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
      subject = s"Reset your lichess.org password, ${user.username}",
      text = s"""
        We received a request to reset the password for your account, ${user.username}.

        If you made this request, click the link below. If you didn't make this request, you can ignore this email.

        $url

        (Clicking not working? Try pasting it into your browser!)

        This message is a service email related to your use of lichess.org.
        """,
      htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">We received a request to reset the password for your account, ${user.username}.</p>
  <p>If you made this request, click the link below. If you didn't make this request, you can ignore this email.</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Reset password">
    <meta itemprop="url" content="$url">
    <p><a itemprop="target" href="$url">$url</a></p>
    <p>(Clicking not working? Try pasting it into your browser!)</p>
  </div>
  <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
    <small>This is a service email related to your use of <a itemprop="url" href="https://lichess.org/"><span itemprop="name">lichess.org</span></a>.</small>
  </div>
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
