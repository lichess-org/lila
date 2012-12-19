package lila
package cli

import lila.wiki.WikiEnv
import scalaz.effects._

private[cli] case class Wiki(env: WikiEnv) {

  def fetch: IO[Unit] = for {
    _ ← putStrLn("Fetching wiki from github")
    _ ← env.fetch.apply
  } yield ()
}
