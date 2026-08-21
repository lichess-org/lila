package lila.fide

import org.apache.pekko.stream.contrib.ZipInputStreamSource
import org.apache.pekko.stream.scaladsl.*
import chess.{ FideId, FideTC, PlayerName, PlayerTitle }
import chess.rating.{ Elo, KFactor }
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.api.bson.*
import java.util.zip.ZipInputStream
import java.time.YearMonth
import scala.collection.mutable.ArrayBuilder

import lila.mon.extensions.*
import lila.core.fide.Federation
import lila.db.dsl.{ *, given }

final private class FidePlayerSync(
    repo: FideRepo,
    ws: StandaloneWSClient,
    proxy: lila.memo.HttpProxy,
    listUrl: Url,
    ratingListsUrl: Url
)(using
    Executor,
    org.apache.pekko.stream.Materializer
):
  import FidePlayerSync.*

  def apply(): Funit = {
    for
      inactiveIds <- inactivityFromHttpFiles()
      _ <- playersFromHttpFile(inactiveIds)
      _ <- federationsFromPlayers()
    yield ()
  }.logFailure(logger)

  // the files are big. We want to stream the http response into the zip reader,
  // and stream the zip output line by line. Don't load the whole thing in memory.
  private def lineSource(url: Url): Fu[Source[String, ?]] = for
    req = ws.url(url.value)
    proxyServer = proxy.select()
    _ = logger.info(s"FidePlayerSync connecting to $url through ${proxyServer.map(_.host)}")
    proxied = proxyServer.foldLeft(req)(_ withProxyServer _)
    httpStream <- proxied.stream()
    _ <-
      if httpStream.status != 200
      then fufail(s"FidePlayerSync $url ${httpStream.status} ${httpStream.statusText}")
      else funit
  yield
    logger.info(s"FidePlayerSync connected to $url")
    ZipInputStreamSource: () =>
      ZipInputStream(httpStream.bodyAsSource.runWith(StreamConverters.asInputStream()))
    .map(_._2)
      .via(Framing.delimiter(org.apache.pekko.util.ByteString("\r\n"), maximumFrameLength = 200))
      .map(_.utf8String)
      .drop(1) // first line is a header

  private object inactivityFromHttpFiles:

    def apply(): Fu[Map[FideTC, InactiveIds]] =
      FideTC.values.toList.sequentially(tc => fetch(tc).dmap(tc -> _)).dmap(_.toMap)

    private def fetch(tc: FideTC): Fu[InactiveIds] = for
      source <- lineSource(Url(s"${ratingListsUrl.value}${tc}_rating_list.zip"))
      (builder, nbLines) <- source.runFold(ArrayBuilder.ofInt() -> 0):
        case ((builder, nbLines), line) =>
          parseInactiveId(line).foreach(builder.addOne)
          (builder, nbLines + 1)
      _ <-
        if nbLines < 100_000
        then
          // As of 2026-08-21, the smallest rating list (blitz) has over 300k lines,
          // so this is a broken download.
          fufail(s"FidePlayerSync the $tc rating list only has $nbLines lines")
        else funit
      ids = builder.result()
    yield
      java.util.Arrays.sort(ids)
      val inactive = InactiveIds(ids)
      logger.info(s"FidePlayerSync $tc: ${inactive.size} inactive players out of $nbLines")
      inactive

  private object federationsFromPlayers:
    def apply(): Funit = for
      feds <- repo.playerColl
        .aggregateList(500, _.sec): framework =>
          import framework.*
          Match(repo.player.selectActive(FideTC.standard)) ->
            List(PipelineOperator($doc("$sortByCount" -> "$fed")))
        .map: objs =>
          for
            obj <- objs
            code <- obj.getAsOpt[Federation.Id]("_id")
            name <- lila.fide.Federation.names.get(code).map(_._1)
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
              active = Match(repo.player.selectActive(tc))
              facet <- List(
                "top" -> List(
                  active,
                  Project($doc("_id" -> 0, tc.toString -> 1)),
                  Sort(Descending(tc.toString)),
                  Limit(10),
                  Group(BSONString(s"$tc-top"))("v" -> AvgField(tc.toString))
                ),
                "count" -> List(
                  active,
                  Match(tc.toString.$exists(true)),
                  Group(BSONString(s"$tc-count"))("v" -> SumAll)
                )
              )
            yield s"$tc-${facet._1}" -> facet._2
            Match($doc("fed" -> code)) ->
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
    def apply(inactiveIds: Map[FideTC, InactiveIds]): Funit = for
      source <- lineSource(listUrl)
      nbUpdated <- source
        .map(parseLine(inactiveIds))
        .mapConcat(_.toList)
        .filter(validatePlayer)
        .grouped(200)
        .map(_.toList)
        .mapAsync(1)(saveIfChanged)
        .runWith(lila.common.LilaStream.sinkSum)
        .monSuccess(lila.mon.fideSync.time)
      nbAll <- repo.player.countAll
    yield
      lila.mon.fideSync.updated.update(nbUpdated)
      lila.mon.fideSync.players.update(nbAll.toDouble)
      logger.info(s"FidePlayerSync upserted: $nbUpdated, total: $nbAll")

    private def saveIfChanged(players: List[FidePlayer]): Future[Int] =
      repo.player
        .fetch(players.map(_.id))
        .flatMap: inDb =>
          val inDbMap: Map[FideId, FidePlayer] = inDb.mapBy(_.id)
          val changed = players.flatMap: fromFide =>
            val inDb = inDbMap.get(fromFide.id)
            inDb
              .forall(i => !i.isSame(fromFide))
              .option:
                fromFide.copy(photo = inDb.flatMap(_.photo))
          changed.nonEmpty.so:
            val update = repo.playerColl.update(ordered = false)
            for
              elements <- changed.sequentially: p =>
                update.element(
                  q = $id(p.id),
                  u = repo.player.handler.writeOpt(p).get,
                  upsert = true
                )
              _ <- elements.nonEmpty.so(update.many(elements).void)
              _ <- updateRatingHistories(changed)
            yield elements.size

    private def updateRatingHistories(players: List[FidePlayer]): Funit =
      val now = YearMonth.now
      players.sequentiallyVoid: p =>
        repo.rating.set(p.id, now, p.ratingsMap)

private object FidePlayerSync:

  final class InactiveIds(sorted: Array[Int]):
    def apply(id: FideId): Boolean = java.util.Arrays.binarySearch(sorted, id.value) >= 0
    def size = sorted.length

  /* a line of a single time control rating list. The flag column is last,
   * and holds "", "i" (inactive), "w" (woman) or "wi".
703303         Leko, Peter                                                  HUN M   GM                           2738  0   10 1979  i
6502938        Acevedo Mendez, Lisseth                                      ISL F   WIM  WIM                     1740  0   20 1993  wi
   */
  def parseInactiveId(line: String): Option[Int] =
    line.drop(132).contains("i").so(line.take(15).trim.toIntOption)

  /* a line of the combined players list, which holds the three ratings but no usable flag
6502938        Acevedo Mendez, Lisseth                                      ISL F   WIM  WIM                     1795  0   20 1767  14  20 1740  0   20 1993  w
6504450        Acevedo Mendez, Oscar                                        CRC M                                1779  0   40              1640  0   20 1994  i
   */
  def parseLine(inactiveIds: Map[FideTC, InactiveIds])(line: String): Option[FidePlayer] =
    def char(at: Int) = line.substring(at, at + 1).headOption
    def string(start: Int, end: Int) = line.substring(start, end).trim.nonEmptyOption
    def number(start: Int, end: Int) = string(start, end).flatMap(_.toIntOption)
    def rating(start: Int) = Elo.from(number(start, start + 4).filter(_ >= 1400))
    def kFactor(start: Int) = KFactor.from(number(start, start + 2).filter(_ > 0))
    val nowYear = nowDateTime.getYear
    for
      id <- number(0, 15).map(FideId(_))
      name1 <- string(15, 76)
      name = name1.trim
      if name.sizeIs > 2
      title = string(84, 89).flatMap(PlayerTitle.get)
      wTitle = string(89, 105).flatMap(PlayerTitle.get)
      year = number(152, 156).filter(_ > 1000).filter(_ < nowYear)
      token = FidePlayer.tokenize.exec(name)
      if token.sizeIs > 2
    yield FidePlayer(
      id = id,
      name = PlayerName(name),
      token = token,
      photo = none,
      fed = Federation.Id.from(string(76, 79).map(_.toUpperCase).filter(_ != "NON")),
      title = PlayerTitle.mostValuable(title, wTitle),
      standard = rating(113),
      standardK = kFactor(123),
      rapid = rating(126),
      rapidK = kFactor(136),
      blitz = rating(139),
      blitzK = kFactor(149),
      year = year,
      gender = FidePlayer.Gender.from(char(80)),
      inactive = FideTC.values.view.filter(tc => inactiveIds(tc)(id)).toSet
    )

  def validatePlayer(p: FidePlayer): Boolean =
    p.age.exists: age =>
      age > 9 || (age > 5 && p.ratingsMap.nonEmpty)
