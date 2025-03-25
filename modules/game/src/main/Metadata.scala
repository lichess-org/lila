package lila.game

import chess.format.pgn.PgnStr

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

object PgnImport:

  def hash(pgn: PgnStr) = // ByteArray {
    MessageDigest
      .getInstance("MD5")
      .digest:
        pgn.value.linesIterator
          .map(_.replace(" ", ""))
          .filter(_.nonEmpty)
          .to(List)
          .mkString("\n")
          .getBytes(UTF_8)
      .take(12)

  def make(user: Option[UserId], date: Option[String], pgn: PgnStr) =
    lila.core.game.PgnImport(
      user = user,
      date = date,
      pgn = pgn,
      h = hash(pgn).some
    )
