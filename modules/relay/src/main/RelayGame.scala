package lila.relay

import org.joda.time.DateTime

import ornicar.scalalib.Random

case class RelayGame(
  id: String, // lichess game ID
  ficsId: Int,
  white: String,
  black: String)

object RelayGame {

  def make(ficsId: Int, white: String, black: String) = RelayGame(
    id = Random nextStringUppercase 8,
    ficsId = ficsId,
    white = white,
    black = black)
}
