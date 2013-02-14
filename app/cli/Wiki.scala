package lila
package cli

import lila.wiki.WikiEnv
import scalaz.effects._

private[cli] case class Wiki(env: WikiEnv) {

  def fetch = env.fetch.apply inject "Fetched wiki from github"
}
