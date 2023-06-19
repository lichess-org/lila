package lila.security

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.common.config.*
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails as trans }
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }
import lila.user.Me

final class PasswordReset(
    mailer: Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor):

  import Mailer.html.*

  def send(user: User, email: EmailAddress)(using lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.send.resetPassword.increment()
      val url = s"$baseUrl/password/reset/confirm/$token"
      mailer send Mailer.Message(
        to = email,
        subject = trans.passwordReset_subject.txt(user.username),
        text = Mailer.txt.addServiceNote(s"""
${trans.passwordReset_intro.txt()}

${trans.passwordReset_clickOrIgnore.txt()}

$url

${trans.common_orPaste.txt()}"""),
        htmlBody = emailMessage(
          pDesc(trans.passwordReset_intro()),
          p(trans.passwordReset_clickOrIgnore()),
          potentialAction(metaName("Reset password"), Mailer.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[Me]] =
    tokener read token flatMapz userRepo.me map {
      _.filter(_.canFullyLogin)
    }

  private val tokener = StringToken[UserId](
    secret = tokenerSecret,
    getCurrentValue = id =>
      for
        hash  <- userRepo getPasswordHash id
        email <- userRepo email id
      yield ~hash + email.fold("")(_.value)
  )
