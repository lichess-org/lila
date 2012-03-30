package lila.cli

import scalaz.effects._

trait Command {

  def apply(): IO[Unit]
}
