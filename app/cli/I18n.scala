package lila
package cli

import lila.parseIntOption
import lila.i18n.I18nEnv
import play.api.i18n.Lang
import java.io._
import scalaz.effects._

private[cli] case class I18n(i18n: I18nEnv) {

  def jsDump: IO[Unit] = for {
    _ ← putStrLn("Dumping JavaScript translations")
    _ ← i18n.jsDump.apply
  } yield ()

  def fileFix: IO[Unit] = for {
    _ ← putStrLn("Fixing translation files")
    _ ← i18n.fileFix.apply
  } yield ()

  def fetch(from: String): IO[Unit] = for {
    _ ← putStrLn("Fetch translations from upstream")
    translations ← i18n.upstreamFetch apply parseIntOption(from).err("bad from arg")
    _ ← i18n.gitWrite apply translations
  } yield ()
}
