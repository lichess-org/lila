package lidraughts.relay

import akka.actor._
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import io.lemonlabs.uri.Url
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.{ WS, WSResponse }
import play.api.Play.current
import scala.concurrent.duration._

import draughts.format.FEN
import lidraughts.base.LidraughtsException
import lidraughts.game.{ GameRepo, PdnDump }
import lidraughts.study.MultiPdn
import lidraughts.tree.Node.Comments
import Relay.Sync.Upstream

private final class RelayFetch(
    sync: RelaySync,
    api: RelayApi,
    formatApi: RelayFormatApi,
    chapterRepo: lidraughts.study.ChapterRepo,
    simulFetch: String => Fu[Option[lidraughts.simul.Simul]],
    pdnDump: PdnDump
) extends Actor {

  override def preStart: Unit = {
    logger.info("Start RelaySync")
    context setReceiveTimeout 20.seconds
    context.system.scheduler.scheduleOnce(10.seconds)(scheduleNext)
  }

  case object Tick

  def scheduleNext =
    context.system.scheduler.scheduleOnce(600 millis, self, Tick)

  def receive = {

    case ReceiveTimeout =>
      val msg = "RelaySync timed out!"
      logger.error(msg)
      throw new RuntimeException(msg)

    case Tick => api.toSync.flatMap { relays =>
      lidraughts.mon.relay.ongoing(relays.size)
      relays.map { relay =>
        if (relay.sync.ongoing) processRelay(relay) flatMap { newRelay =>
          api.update(relay)(_ => newRelay)
        }
        else if (relay.hasStarted) {
          logger.info(s"Finish by lack of activity $relay")
          api.update(relay)(_.finish)
        } else if (relay.shouldGiveUp) {
          logger.info(s"Finish for lack of start $relay")
          api.update(relay)(_.finish)
        } else fuccess(relay)
      }.sequenceFu addEffectAnyway scheduleNext
    }
  }

  // no writing the relay; only reading!
  def processRelay(relay: Relay): Fu[Relay] =
    if (!relay.sync.playing) fuccess(relay.withSync(_.play))
    else doProcess(relay) flatMap { games =>
      sync(relay, games)
        .chronometer.mon(_.relay.sync.duration.each).result
        .withTimeout(1500 millis, SyncResult.Timeout)(context.system) map { res =>
          res -> relay.withSync(_ addLog SyncLog.event(res.moves, none))
        }
    } recover {
      case e: Exception => (e match {
        case res @ SyncResult.Timeout =>
          logger.info(s"Sync timeout $relay")
          res
        case _ =>
          logger.info(s"Sync error $relay ${e.getMessage take 80}")
          SyncResult.Error(e.getMessage)
      }) -> relay.withSync(_ addLog SyncLog.event(0, e.some))
    } flatMap {
      case (result, newRelay) => afterSync(result, newRelay)
    }

  def afterSync(result: SyncResult, relay: Relay): Fu[Relay] = {
    lidraughts.mon.relay.sync.result(result.reportKey)()
    result match {
      case SyncResult.Ok(0, games) =>
        if (games.size > 1 && games.forall(_.finished)) {
          logger.info(s"Finish because all games are over $relay")
          fuccess(relay.finish)
        } else continueRelay(relay)
      case SyncResult.Ok(nbMoves, games) =>
        lidraughts.mon.relay.moves(nbMoves)
        continueRelay(relay.ensureStarted.resume)
      case _ => continueRelay(relay)
    }
  }

  def continueRelay(r: Relay): Fu[Relay] = {
    if (r.sync.log.alwaysFails && !r.sync.upstream.isLocal) fuccess(60)
    else r.sync.delay match {
      case Some(delay) => fuccess(delay)
      case None => api getNbViewers r map { nb =>
        (21 - nb) atLeast 10
      }
    }
  } map { seconds =>
    r.withSync {
      _.copy(
        nextAt = DateTime.now plusSeconds {
          seconds atLeast { if (r.sync.log.justTimedOut) 10 else 2 }
        } some
      )
    }
  }

  import RelayFetch.GamesSeenBy

  private def doProcess(relay: Relay): Fu[RelayGames] =
    if (relay.sync.simulId.isDefined) doFetchSimul(relay.sync.simulId.get, RelayFetch.maxChapters(relay))
    // different indices may be fetched from the same upstream, so bypass cache
    else if (relay.sync.indices.exists(_.nonEmpty)) doFetchByIndex(relay.sync.upstream, relay.sync.indices.get, RelayFetch.maxChapters(relay))
    else cache getIfPresent relay.sync.upstream match {
      case Some(GamesSeenBy(games, seenBy)) if !seenBy(relay.id) =>
        cache.put(relay.sync.upstream, GamesSeenBy(games, seenBy + relay.id))
        games
      case _ =>
        val games = doFetch(relay.sync.upstream, RelayFetch.maxChapters(relay))
        cache.put(relay.sync.upstream, GamesSeenBy(games, Set(relay.id)))
        games
    }

  private val cache: Cache[Upstream, GamesSeenBy] = Scaffeine()
    .expireAfterWrite(30.seconds)
    .build[Upstream, GamesSeenBy]

  private val pdnFlags = PdnDump.WithFlags(
    evals = false,
    opening = false
  )

  private def doFetchSimul(simulId: String, max: Int): Fu[RelayGames] =
    simulFetch(simulId) flatMap {
      _ ?? { simul =>
        simul.isStarted ?? GameRepo.gamesFromPrimary(simul.gameIds).map(games => (simul, games).some)
      }
    } flatMap {
      case Some((simul, games)) =>
        games.zipWithIndex.map {
          case (game, i) =>
            val number = i + 1
            pdnDump(game, FEN(game.variant.initialFen).some, pdnFlags) map {
              number -> _.withEvent(s"${simul.fullName} https://lidraughts.org/simul/${simul.id}")
            }
        } sequenceFu
      case _ => fuccess(Nil)
    } map { results =>
      MultiPdn(results.sortBy(_._1).take(max).map(_._2.render))
    } flatMap RelayFetch.multiPdnToGames.apply

  private def doFetchByIndex(upstream: Upstream, indices: List[Int], max: Int): Fu[RelayGames] =
    indices.map { i: Int =>
      httpGet(Url.parse(s"${upstream.url}$i.PDN")) map (i -> _)
    }.sequenceFu.map { results =>
      MultiPdn(results.sortBy(_._1).take(max).map(_._2))
    } flatMap RelayFetch.multiPdnToGames.apply

  private def doFetch(upstream: Upstream, max: Int): Fu[RelayGames] = {
    import RelayFetch.DgtJson._
    formatApi.get(upstream.url) flatMap {
      case RelayFormat.SingleFile(doc) => doc.format match {
        case RelayFormat.DocFormat.Pdn => httpGet(doc.url) map { pdns => MultiPdn.split(pdns.replace("<br>", "\n\n"), max) }
        case RelayFormat.DocFormat.Json => httpGetJson[GamesJson](doc.url)(gamesReads) map { games =>
          MultiPdn(games.toPdns)
        }
      }
      case RelayFormat.ManyFiles(indexUrl, makeGameDoc) => httpGetJson[RoundJson](indexUrl) flatMap { round =>
        round.pairings.zipWithIndex.map {
          case (pairing, i) =>
            val number = i + 1
            val gameDoc = makeGameDoc(number)
            (gameDoc.format match {
              case RelayFormat.DocFormat.Pdn => httpGet(gameDoc.url)
              case RelayFormat.DocFormat.Json => httpGetJson[GameJson](gameDoc.url) map { _.pdn.getOrElse("") }
            }) map (number -> _)
        }.sequenceFu.map { results =>
          MultiPdn(results.sortBy(_._1).map(_._2).toList)
        }
      }
    } flatMap RelayFetch.multiPdnToGames.apply
  }

  private def httpGet(url: Url): Fu[String] =
    WS.url(url.toString).withRequestTimeout(4.seconds.toMillis).get().flatMap {
      case res if res.status == 200 => fuccess(res.body)
      case res => fufail(s"[${res.status}] $url")
    }

  private def httpGetJson[A: Reads](url: Url): Fu[A] = for {
    str <- httpGet(url)
    json <- scala.concurrent.Future(Json parse str) // Json.parse throws exceptions (!)
    data <- implicitly[Reads[A]].reads(json).fold(
      err => fufail(s"Invalid JSON from $url: $err"),
      fuccess
    )
  } yield data
}

private object RelayFetch {

  case class GamesSeenBy(games: Fu[RelayGames], seenBy: Set[Relay.Id])

  def maxChapters(relay: Relay) =
    lidraughts.study.Study.maxChapters * (if (relay.official) 2 else 1)

  private object DgtJson {
    case class PairingPlayer(fname: Option[String], mname: Option[String], lname: Option[String], title: Option[String]) {
      def fullName = some {
        List(fname, mname, lname).flatten mkString " "
      }.filter(_.nonEmpty)
    }
    case class RoundJsonPairing(white: PairingPlayer, black: PairingPlayer, result: String) {
      import draughts.format.pdn._
      def tags = Tags(List(
        white.fullName map { v => Tag(_.White, v) },
        white.title map { v => Tag(_.WhiteTitle, v) },
        black.fullName map { v => Tag(_.Black, v) },
        black.title map { v => Tag(_.BlackTitle, v) },
        Tag(_.Result, result).some
      ).flatten)
    }
    case class RoundJson(pairings: List[RoundJsonPairing])
    implicit val pairingPlayerReads = Json.reads[PairingPlayer]
    implicit val roundPairingReads = Json.reads[RoundJsonPairing]
    implicit val roundReads = Json.reads[RoundJson]

    case class GameJson(pdn: Option[String], result: Option[String])
    case class GamesJson(games: List[GameJson]) {
      def toPdns = games.flatMap {
        _.pdn.map(p => {
          def nonemptyPdn = if (p == "[Empty]") "" else p
          nonemptyPdn.replace("\\n", "\n").replace("\\\"", "\"")
        })
      }
    }
    implicit val gameReads = Json.reads[GameJson]
    implicit val gamesReads = Json.reads[GamesJson]
  }
  import DgtJson._

  private object multiPdnToGames {

    import scala.util.{ Try, Success, Failure }
    import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }

    def apply(multiPdn: MultiPdn): Fu[List[RelayGame]] =
      multiPdn.value.foldLeft[Try[(List[RelayGame], Int)]](Success(List.empty -> 0)) {
        case (Success((acc, index)), pdn) => pdnCache.get(pdn) flatMap { f =>
          val game = f(index)
          if (game.isEmpty) Failure(LidraughtsException(s"Found an empty PDN at index $index"))
          else Success(game :: acc, index + 1)
        }
        case (acc, _) => acc
      }.future.map(_._1.reverse)

    private val pdnCache: LoadingCache[String, Try[Int => RelayGame]] = Scaffeine()
      .expireAfterAccess(2 minutes)
      .build(compute)

    private def compute(pdn: String): Try[Int => RelayGame] =
      lidraughts.study.PdnImport(pdn, Nil, lidraughts.pref.Pref.default.draughtsResult).fold(
        err => Failure(LidraughtsException(err)),
        res => Success(index => RelayGame(
          index = index,
          tags = res.tags,
          variant = res.variant,
          root = res.root.copy(
            comments = Comments.empty,
            children = res.root.children.updateMainline(_.copy(comments = Comments.empty))
          ),
          end = res.end
        ))
      )
  }
}
