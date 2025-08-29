package lila.fide

import akka.stream.contrib.ZipInputStreamSource
import akka.stream.scaladsl.*
import chess.{ FideId, PlayerName, PlayerTitle }
import chess.rating.{ Elo, KFactor }
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.api.bson.*
import java.util.zip.ZipInputStream

import lila.core.fide.{ Federation, FideTC }
import lila.db.dsl.{ *, given }

final private class FidePlayerSync(repo: FideRepo, ws: StandaloneWSClient)(using
    Executor,
    akka.stream.Materializer
):

  private val listUrl = "http://ratings.fide.com/download/players_list.zip"

  // the file is big. We want to stream the http response into the zip reader,
  // and stream the zip output into the database as it's being extracted.
  // Don't load the whole thing in memory.
  def apply(): Funit = for
    _ <- playersFromHttpFile()
    _ <- federationsFromPlayers()
  yield ()

  private object federationsFromPlayers:
    def apply(): Funit = for
      feds <- repo.playerColl
        .aggregateList(500, _.sec): framework =>
          import framework.*
          Match(repo.player.selectActive) ->
            List(PipelineOperator($doc("$sortByCount" -> "$fed")))
        .map: objs =>
          for
            obj <- objs
            code <- obj.getAsOpt[Federation.Id]("_id")
            name <- lila.fide.Federation.names.get(code)
            nbPlayers <- obj.int("count")
            if nbPlayers >= 5
          yield (code, name, nbPlayers)
      // TODO https://www.mongodb.com/docs/manual/reference/operator/aggregation/topN/
      federations <- feds.traverse: (code, name, nbPlayers) =>
        repo.playerColl
          .aggregateOne(_.sec): framework =>
            import framework.*
            val facets = for
              tc <- FideTC.values.toList
              facet <- List(
                "top" -> List(
                  Project($doc("_id" -> 0, tc.toString -> 1)),
                  Sort(Descending(tc.toString)),
                  Limit(10),
                  Group(BSONString(s"$tc-top"))("v" -> AvgField(tc.toString))
                ),
                "count" -> List(
                  Match(tc.toString.$exists(true)),
                  Group(BSONString(s"$tc-count"))("v" -> SumAll)
                )
              )
            yield s"$tc-${facet._1}" -> facet._2
            Match(repo.player.selectActive ++ $doc("fed" -> code)) ->
              List(
                Facet(facets),
                Project($doc("all" -> $doc("$setUnion" -> facets.map((k, _) => s"$$$k").toList))),
                UnwindField("all"),
                ReplaceRootField("all"),
                Project($doc("k" -> "$_id", "v" -> true, "_id" -> false)),
                Group(BSONNull)("all" -> PushField("$ROOT")),
                Project($doc("_id" -> $doc("$arrayToObject" -> "$all"))),
                ReplaceRootField("_id")
              )
          .map2: o =>
            def stats(tc: FideTC) = Federation.Stats(
              rank = 0,
              nbPlayers = ~o.int(s"$tc-count"),
              top10Rating = ~o.double(s"$tc-top").map(_.toInt)
            )
            lila.fide.Federation(
              id = code,
              name = name,
              nbPlayers = nbPlayers,
              standard = stats(FideTC.standard),
              rapid = stats(FideTC.rapid),
              blitz = stats(FideTC.blitz),
              updatedAt = nowInstant
            )
      ranked = FideTC.values.foldLeft(federations.flatten): (acc, tc) =>
        acc
          .sortBy(-_.stats(tc).get.top10Rating)
          .zipWithIndex
          .map: (fed, index) =>
            fed.stats(tc).modify(_.copy(rank = index + 1))
      _ <- ranked.sequentially(repo.federation.upsert)
    yield ()

  private object playersFromHttpFile:
    def apply(): Funit = for
      httpStream <- ws.url(listUrl).stream()
      _ <-
        if httpStream.status != 200 then
          fufail(s"RelayFidePlayerApi.pull ${httpStream.status} ${httpStream.statusText}")
        else
          for
            nbUpdated <-
              ZipInputStreamSource: () =>
                ZipInputStream(httpStream.bodyAsSource.runWith(StreamConverters.asInputStream()))
              .map(_._2)
                .via(Framing.delimiter(akka.util.ByteString("\r\n"), maximumFrameLength = 200))
                .map(_.utf8String)
                .drop(1) // first line is a header
                .map(parseLine)
                .mapConcat(_.toList)
                .grouped(200)
                .mapAsync(1)(saveIfChanged)
                .runWith(lila.common.LilaStream.sinkSum)
                .monSuccess(_.fideSync.time)
            nbAll <- repo.player.countAll
          yield
            lila.mon.fideSync.updated.update(nbUpdated)
            lila.mon.fideSync.players.update(nbAll.toDouble)
            logger.info(s"RelayFidePlayerApi.update upserted: $nbUpdated, total: $nbAll")
    yield ()

    /*
  6502938        Acevedo Mendez, Lisseth                                      ISL F   WIM  WIM                     1795  0   20 1767  14  20 1740  0   20 1993  w
  6504450        Acevedo Mendez, Oscar                                        CRC M                                1779  0   40              1640  0   20 1994  i
     */
    private def parseLine(line: String): Option[FidePlayer] =
      def string(start: Int, end: Int) = line.substring(start, end).trim.some.filter(_.nonEmpty)
      def number(start: Int, end: Int) = string(start, end).flatMap(_.toIntOption)
      def rating(start: Int) = Elo.from(number(start, start + 4).filter(_ >= 1400))
      def kFactor(start: Int) = KFactor.from(number(start, start + 2).filter(_ > 0))
      val nowYear = nowDateTime.getYear
      for
        id <- number(0, 15)
        name1 <- string(15, 76)
        name = name1.trim
        if name.sizeIs > 2
        title = string(84, 89).flatMap(PlayerTitle.get)
        wTitle = string(89, 105).flatMap(PlayerTitle.get)
        year = number(152, 156).filter(_ > 1000).filter(_ < nowYear)
        flags = string(158, 160)
        token = FidePlayer.tokenize(name)
        if token.sizeIs > 2
      yield FidePlayer(
        id = FideId(id),
        name = PlayerName(name),
        token = token,
        fed = Federation.Id.from(string(76, 79).map(_.toUpperCase).filter(_ != "NON")),
        title = PlayerTitle.mostValuable(title, wTitle),
        standard = rating(113),
        standardK = kFactor(123),
        rapid = rating(126),
        rapidK = kFactor(136),
        blitz = rating(139),
        blitzK = kFactor(149),
        year = year,
        inactive = flags.exists(_.contains("i"))
      )

    private def saveIfChanged(players: Seq[FidePlayer]): Future[Int] =
      repo.player
        .fetch(players.map(_.id))
        .flatMap: inDb =>
          val inDbMap: Map[FideId, FidePlayer] = inDb.mapBy(_.id)
          val changed = players.filter: p =>
            inDbMap.get(p.id).fold(true)(i => !i.isSame(p))
          changed.nonEmpty.so:
            val update = repo.playerColl.update(ordered = false)
            for
              elements <- changed.toList.sequentially: p =>
                update.element(
                  q = $id(p.id),
                  u = repo.player.handler.writeOpt(p).get,
                  upsert = true
                )
              _ <- elements.nonEmpty.so(update.many(elements).void)
            yield elements.size
