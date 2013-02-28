package lila.app
package cli

import lila.app.parseIntOption
import lila.app.i18n.I18nEnv
import play.api.i18n.Lang
import java.io._
import scalaz.effects._

private[cli] case class I18n(i18n: I18nEnv) {

  def jsDump: IO[String] = for {
    _ ← i18n.jsDump.apply
  } yield "Dumped JavaScript translations"

  def fileFix: IO[String] = for {
    _ ← i18n.fileFix.apply
  } yield "Fixed translation files"

  def fetch(from: String): IO[String] = for {
    translations ← i18n.upstreamFetch apply parseIntOption(from).err("bad from arg")
    _ ← i18n.gitWrite apply translations
  } yield "Fetched translations from upstream"
}
