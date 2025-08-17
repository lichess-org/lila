package lila.mailer

import akka.actor.ActorSystem
import play.api.ConfigLoader
import play.api.libs.mailer.{ Email, SMTPConfiguration, SMTPMailer }
import scalatags.Text.all.{ html as htmlTag, * }
import scalatags.Text.tags2.title as titleTag
import org.apache.commons.mail.EmailException

import scala.concurrent.blocking

import lila.common.String.html.nl2br
import lila.common.autoconfig.*
import lila.core.i18n.I18nKey.emails as trans
import lila.core.i18n.Translate

final class Mailer(
    config: Mailer.Config,
    canSendEmails: lila.memo.SettingStore[Boolean],
    getSecondaryPermille: () => Int
)(using system: ActorSystem, scheduler: Scheduler):

  private given blockingExecutor: Executor =
    system.dispatchers.lookup("blocking-smtp-dispatcher")

  private val primaryClient = SMTPMailer(config.primary.toClientConfig)
  private val secondaryClient = SMTPMailer(config.secondary.toClientConfig)

  private def randomClientFor(recipient: EmailAddress): (SMTPMailer, Mailer.Smtp) =
    // Stick to one mailer for each recipient, because each mailer may have its
    // own supression list.
    if recipient.normalize.value.hashCode.abs % 1000 < getSecondaryPermille() then
      (secondaryClient, config.secondary)
    else (primaryClient, config.primary)

  def canSend = canSendEmails.get()

  def sendOrFail(msg: Mailer.Message): Funit = send(msg, orFail = true, retry = Mailer.Retry(3))
  def sendOrSkip(msg: Mailer.Message): Funit = send(msg, orFail = false, retry = Mailer.Retry(0))

  private def send(msg: Mailer.Message, orFail: Boolean, retry: Mailer.Retry): Funit =
    if !canSend then
      logger.warn("Can't send any emails due to live setting")
      funit
    else if msg.to.isNoReply then
      logger.warn(s"Can't send ${msg.subject} to noreply email ${msg.to}")
      funit
    else
      Future:
        val (client, config) = randomClientFor(msg.to)
        val email = Email(
          subject = msg.subject,
          from = config.sender,
          to = Seq(msg.to.value),
          bodyText = msg.text.some,
          bodyHtml = msg.htmlBody.map { body => Mailer.html.wrap(msg.subject, body).render }
        )
        blocking:
          client.send(email)
      .monSuccess(_.email.send.time)
        .recoverWith:
          case _: EmailException if msg.to.normalize.value != msg.to.value =>
            logger.warn(s"Email ${msg.to} is invalid, trying ${msg.to.normalize}")
            send(msg.copy(to = msg.to.normalize.into(EmailAddress)), orFail, retry)
          case e: Exception =>
            retry.again match
              case None if orFail => throw e
              case None =>
                logger.warn(s"Couldn't send email to ${msg.to}: ${e.getMessage}")
                funit
              case Some(nextTry) =>
                logger.info(s"Will retry to send email to ${msg.to} after: ${e.getMessage}")
                scheduler.scheduleOnce(nextTry.delay)(send(msg, orFail, nextTry))
                funit
        .void

object Mailer:

  private val timeout = 5.seconds

  case class Retry(times: Int, delay: FiniteDuration = 20.seconds):
    def again = Option.when(times > 0)(Retry(times - 1, delay * 2))

  case class Smtp(
      mock: Boolean,
      host: String,
      port: Int,
      tls: Boolean,
      user: String,
      sender: String,
      password: String
  ):
    def toClientConfig = SMTPConfiguration(
      host = host,
      port = port,
      tlsRequired = tls,
      user = user.some,
      password = password.some,
      mock = mock,
      timeout = Mailer.timeout.toMillis.toInt.some
    )
  given ConfigLoader[Smtp] = AutoConfig.loader[Smtp]

  case class Config(primary: Smtp, secondary: Smtp)
  given ConfigLoader[Config] = AutoConfig.loader[Config]

  case class Message(
      to: EmailAddress,
      subject: String,
      text: String,
      htmlBody: Option[Frag] = none
  )

  object txt:

    private def serviceNote(using Translate): String = s"""
${trans.common_note("https://lichess.org").render}

${trans.common_contact("https://lichess.org/contact").render}"""

    def addServiceNote(body: String)(using Translate) = s"""$body

$serviceNote"""

  object html:

    private val itemscope = attr("itemscope").empty
    private val itemtype = attr("itemtype")
    private val itemprop = attr("itemprop")

    val emailMessage = div(itemscope, itemtype := "http://schema.org/EmailMessage")
    val pDesc = p(itemprop := "description")
    val potentialAction =
      div(itemprop := "potentialAction", itemscope, itemtype := "http://schema.org/ViewAction")
    def metaName(cont: String) = meta(itemprop := "name", content := cont)
    val publisher = div(itemprop := "publisher", itemscope, itemtype := "http://schema.org/Organization")
    val noteContact = a(itemprop := "url", href := "https://lichess.org/contact")(
      span(itemprop := "name")("lichess.org/contact")
    )

    private val noteLink = a(
      itemprop := "url",
      href := "https://lichess.org/"
    )(span(itemprop := "name")("lichess.org"))

    def serviceNote(using Translate) =
      publisher(
        small(
          trans.common_note(Mailer.html.noteLink),
          " ",
          trans.common_contact(noteContact),
          " ",
          lila.core.i18n.I18nKey.site.readAboutOur(
            a(href := "https://lichess.org/privacy")(
              lila.core.i18n.I18nKey.site.privacyPolicy()
            )
          )
        )
      )

    def standardEmail(body: String)(using Translate): Frag =
      emailMessage(
        pDesc(nl2br(body)),
        serviceNote
      )

    def url(u: String, clickOrPaste: Boolean = true)(using Translate) =
      frag(
        meta(itemprop := "url", content := u),
        p(a(itemprop := "target", href := u)(u)),
        clickOrPaste.option(p(trans.common_orPaste()))
      )

    private[Mailer] def wrap(subject: String, htmlBody: Frag): Frag =
      frag(
        raw("<!doctype html>"),
        htmlTag(
          head(
            meta(httpEquiv := "Content-Type", content := "text/html; charset=utf-8"),
            meta(name := "viewport", content := "width=device-width"),
            titleTag(subject)
          ),
          body(htmlBody)
        )
      )
