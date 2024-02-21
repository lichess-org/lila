package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import akka.util.ByteString
import akka.stream.scaladsl.*
import akka.stream.Materializer
import akka.stream.contrib.ZipInputStreamSource
import play.api.libs.ws.StandaloneWSClient
import java.io.InputStream
import java.util.zip.ZipInputStream

import lila.db.dsl.{ *, given }

case class RelayFidePlayer(
    @Key("_id") id: Int,
    name: String,
    title: Option[UserTitle],
    standard: Option[IntRating],
    rapid: Option[IntRating],
    blitz: Option[IntRating]
)

final private class RelayFidePlayerApi(colls: RelayColls, ws: StandaloneWSClient)(using
    Executor,
    Materializer
):
  val coll = colls.fidePlayer
  import BSONHandlers.given

  def get(id: Int) = coll(_.byId[RelayFidePlayer](id))

  // the file is big. We want to stream the http response into the zip reader,
  // and stream the zip output into the database as it's being extracted.
  // Don't load the whole thing in memory.
  def update(): Funit =
    ws.url("http://ratings.fide.com/download/players_list.zip").stream() flatMap:
      case res if res.status == 200 =>
        ZipInputStreamSource: () =>
          ZipInputStream(res.bodyAsSource.runWith(StreamConverters.asInputStream()))
        .map(_._2)
          .via(Framing.delimiter(akka.util.ByteString("\r\n"), maximumFrameLength = 200))
          .map(_.utf8String)
          .drop(1) // first line is a header
          .map(lineToPlayer)
          .mapConcat(_.toList)
          .mapAsync(4): player =>
            coll(_.update.one($id(player.id), player, upsert = true))
          .runWith(lila.common.LilaStream.sinkCount)
          .monSuccess(_.relay.fidePlayers.update)
          .addEffect(lila.mon.relay.fidePlayers.nb.update(_))
          .addEffect(nb => logger.info(s"RelayFidePlayerApi.update done $nb"))
          .void
      case res => fufail(s"RelayFidePlayerApi.pull ${res.status} ${res.statusText}")

  /*
6502938        Acevedo Mendez, Lisseth                                      ISL F   WIM  WIM                     1795  0   20 1767  14  20 1740  0   20 1993  w
6504450        Acevedo Mendez, Oscar                                        CRC M                                1779  0   40              1640  0   20 1994  i
   */
  private def lineToPlayer(line: String): Option[RelayFidePlayer] =
    def string(start: Int, end: Int) = line.substring(start, end).trim.some.filter(_.nonEmpty)
    def number(start: Int, end: Int) = string(start, end).flatMap(_.toIntOption)
    def rating(start: Int, end: Int) = number(start, end).map(IntRating(_))
    for
      id   <- number(0, 15)
      name <- string(15, 76)
    yield RelayFidePlayer(
      id = id,
      name = name,
      title = string(84, 89).flatMap(lila.user.Title.get),
      standard = rating(113, 117),
      rapid = rating(126, 132),
      blitz = rating(139, 145)
    )
