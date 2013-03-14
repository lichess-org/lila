package lila.app
package cli

import lila.app.wiki.WikiEnv
import scalaz.effects._

private[cli] case class Wiki(env: WikiEnv) {

  def fetch = env.fetch.apply inject "Fetched wiki from github"
}
