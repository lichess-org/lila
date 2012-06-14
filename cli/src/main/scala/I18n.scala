package lila.cli

import lila.i18n.I18nEnv
import play.api.i18n.Lang
import java.io._
import scalaz.effects._

case class I18n(i18n: I18nEnv) {

  def jsDump: IO[Unit] = for {
    _ ← putStrLn("Dumping JavaScript translations")
    _ ← i18n.jsDump.apply
  } yield ()
}
