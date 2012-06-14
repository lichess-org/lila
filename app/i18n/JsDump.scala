package lila
package i18n

import play.api.i18n.Lang
import java.io._
import scalaz.effects._

case class JsDump(
    path: String,
    pool: I18nPool,
    keys: I18nKeys) {

  private val messages = List(
    keys.unlimited,
    keys.standard,
    keys.rated,
    keys.casual,
    keys.noGameAvailableRightNowCreateOne,
    keys.thisGameIsRated,
    keys.whiteCreatesTheGame,
    keys.blackCreatesTheGame,
    keys.whiteJoinsTheGame,
    keys.blackJoinsTheGame,
    keys.drawOfferSent,
    keys.drawOfferDeclined,
    keys.drawOfferAccepted,
    keys.drawOfferCanceled,
    keys.rematchOfferSent,
    keys.rematchOfferAccepted,
    keys.rematchOfferCanceled,
    keys.rematchOfferDeclined,
    keys.takebackPropositionSent,
    keys.takebackPropositionDeclined,
    keys.takebackPropositionAccepted,
    keys.takebackPropositionCanceled,
    keys.gameOver,
    keys.yourTurn,
    keys.waitingForOpponent)

  private val en = Lang("en")

  private val pathFile = new File(path)

  def apply: IO[Unit] = for {
    _ ← io(pathFile.mkdir)
    _ ← (pool.langs.toList map write).sequence
  } yield ()

  private def write(lang: Lang): IO[Unit] = io {
    val code = dump(lang)
    val file = new File("%s/%s.js".format(pathFile.getCanonicalPath, lang.language))
    val out = new PrintWriter(file)
    try { out.print(code) }
    finally { out.close }
  }

  private def dump(lang: Lang): String =
    """lichess_translations = {%s};""".format(messages map { key ⇒
      """"%s":"%s"""".format(escape(key.to(en)()), escape(key.to(lang)()))
    } mkString ",")

  private def escape(text: String) = text.replace(""""""", """\"""")
}
