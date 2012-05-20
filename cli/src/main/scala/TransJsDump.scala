package lila.cli

import lila.i18n.{ I18nPool, I18nKeys }
import play.api.i18n.Lang
import java.io._
import scalaz.effects._

case class TransJsDump(
    path: File,
    pool: I18nPool,
    keys: I18nKeys) extends Command {

  val messages = List(
    keys.unlimited,
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

  def apply: IO[Unit] = for {
    _ ← putStrLn("Dumping JavaScript translations in " + path)
    langs = pool.langs
    _ ← run(path.mkdir, "Create directory")
    _ ← run(langs foreach { lang ⇒ write(lang) }, "Write translations")
  } yield ()

  def write(lang: Lang) {
    val code = dump(lang)
    val file = new File(
      "%s/%s.js".format(path.getCanonicalPath, lang.language))
    val out = new PrintWriter(file)
    try { out.print(code) }
    finally { out.close }
  }

  def dump(lang: Lang): String =
    """lichess_translations = {%s};""".format(messages map { key ⇒
      """%s:"%s"""".format(key.key, key.to(lang)())
    } mkString ",")

  def run(op: ⇒ Unit, desc: String = "") = for {
    _ ← desc.nonEmpty.fold(putStrLn(desc), io())
    _ ← io { op }
  } yield ()
}
