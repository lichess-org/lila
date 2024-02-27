package lila.fide

import akka.util.ByteString
import akka.stream.scaladsl.*
import akka.stream.contrib.ZipInputStreamSource
import play.api.libs.ws.StandaloneWSClient
import java.io.InputStream
import java.util.zip.ZipInputStream
import chess.format.pgn.{ Tag, Tags }
import chess.{ FideId, ByColor }

import lila.db.dsl.{ *, given }

final private class FidePlayerSync(api: FidePlayerApi, ws: StandaloneWSClient)(using
    Executor,
    akka.stream.Materializer
):

  import FidePlayer.*
  import api.playerHandler

  // the file is big. We want to stream the http response into the zip reader,
  // and stream the zip output into the database as it's being extracted.
  // Don't load the whole thing in memory.
  def apply(): Funit =
    ws.url("http://ratings.fide.com/download/players_list.zip").stream() flatMap:
      case res if res.status == 200 =>
        val startAt = nowInstant
        ZipInputStreamSource: () =>
          ZipInputStream(res.bodyAsSource.runWith(StreamConverters.asInputStream()))
        .map(_._2)
          .via(Framing.delimiter(akka.util.ByteString("\r\n"), maximumFrameLength = 200))
          .map(_.utf8String)
          .drop(1) // first line is a header
          .map(parseLine)
          .mapConcat(_.toList)
          .grouped(100)
          .mapAsync(1)(upsert)
          .runWith(lila.common.LilaStream.sinkCount)
          .monSuccess(_.relay.fidePlayers.update)
          .flatMap: nb =>
            lila.mon.relay.fidePlayers.nb.update(nb)
            unpublishOlderThan(startAt) map: deleted =>
              logger.info(s"RelayFidePlayerApi.update upserted: $nb, deleted: $nb")

      case res => fufail(s"RelayFidePlayerApi.pull ${res.status} ${res.statusText}")

  /*
6502938        Acevedo Mendez, Lisseth                                      ISL F   WIM  WIM                     1795  0   20 1767  14  20 1740  0   20 1993  w
6504450        Acevedo Mendez, Oscar                                        CRC M                                1779  0   40              1640  0   20 1994  i
   */
  private def parseLine(line: String): Option[FidePlayer] =
    def string(start: Int, end: Int) = line.substring(start, end).trim.some.filter(_.nonEmpty)
    def number(start: Int, end: Int) = string(start, end).flatMap(_.toIntOption)
    for
      id   <- number(0, 15)
      name <- string(15, 76)
      title  = UserTitle from string(84, 89)
      wTitle = UserTitle from string(89, 105)
      year   = number(152, 156).filter(_ > 1000)
    yield FidePlayer(
      id = FideId(id),
      name = name,
      token = FidePlayer.tokenize(name),
      fed = string(76, 79),
      title = mostValuable(title, wTitle),
      standard = number(113, 117),
      rapid = number(126, 132),
      blitz = number(139, 145),
      year = year,
      fetchedAt = nowInstant,
      public = true
    )

  // ordered by difficulty to achieve
  // if a player has multiple titles, the most valuable one is used
  private val titleRank: Map[UserTitle, Int] = UserTitle
    .from:
      List("GM", "IM", "WGM", "FM", "WIM", "WFM", "NM", "CM", "WCM", "WNM")
    .zipWithIndex
    .toMap

  private def mostValuable(t1: Option[UserTitle], t2: Option[UserTitle]): Option[UserTitle] =
    t1.flatMap(titleRank.get)
      .fold(t2): v1 =>
        t2.flatMap(titleRank.get)
          .fold(t1): v2 =>
            if v1 < v2 then t1 else t2

  private def upsert(ps: Seq[FidePlayer]) =
    val update = api.coll.update(ordered = false)
    for
      elements <- ps.traverse: p =>
        update.element(
          q = $id(p.id),
          u = api.playerHandler.writeOpt(p).get,
          upsert = true
        )
      _ <- elements.nonEmpty so update.many(elements).void
    yield ()

  private def unpublishOlderThan(date: Instant): Fu[Int] =
    api.coll.update.one($doc("fetchedAt" $lt date), $set("public" -> false), multi = true).map(_.n)
