package lila.game

import chess.format.pgn.PgnStr
import chess.{ Color, Ply }

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import lila.db.ByteArray
import lila.core.game.{ Source, GameRule, GameMetadata, GameDrawOffers, PgnImport }

object PgnImport:

  def hash(pgn: PgnStr) = // ByteArray {
    MessageDigest
      .getInstance("MD5")
      .digest {
        pgn.value.linesIterator
          .map(_.replace(" ", ""))
          .filter(_.nonEmpty)
          .to(List)
          .mkString("\n")
          .getBytes(UTF_8)
      }
      .take(12)

  def make(user: Option[UserId], date: Option[String], pgn: PgnStr) =
    lila.core.game.PgnImport(
      user = user,
      date = date,
      pgn = pgn,
      h = hash(pgn).some
    )
