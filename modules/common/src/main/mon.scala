package lila

import com.github.benmanes.caffeine.cache.{ Cache => CaffeineCache }
import kamon.Kamon._
import kamon.metric.{ Timer, Counter }
import kamon.tag.TagSet

import lila.common.ApiVersion

object mon {

  object http {
    private val t = timer("http.time")
    def time(action: String, client: String, method: String, code: Int) = t.withTags(Map(
      "action" -> action, "client" -> client, "method" -> method, "code" -> code.toLong
    ))
    def error(action: String, client: String, method: String, code: Int) = counter("http.error").withTags(Map(
      "action" -> action, "client" -> client, "method" -> method, "code" -> code.toLong
    ))
    def path(p: String) = counter("http.path.count").withTag("path", p)
    val userGamesCost = counter("http.userGames.cost").withoutTags
    def csrfError(tpe: String, action: String, client: String) =
      counter("http.csrf.error").withTags(Map("type" -> tpe, "action" -> action, "client" -> client))
    val fingerPrint = timer("http.fingerPrint.time").withoutTags
    def jsmon(event: String) = counter("http.jsmon").withTag("event", event)
  }
  object syncache {
    def miss(name: String) = counter("syncache.miss").withTag("name", name)
    def preload(name: String) = counter("syncache.preload").withTag("name", name)
    def timeout(name: String) = counter("syncache.timeout").withTag("name", name)
    def compute(name: String) = timer("syncache.compute").withTag("name", name)
    def wait(name: String) = timer("syncache.wait").withTag("name", name)
  }
  def caffeineStats(cache: CaffeineCache[_, _], name: String): Unit = {
    val stats = cache.stats
    counter("caffeine.request").withTags(Map("name" -> name, "hit" -> true)).increment(stats.hitCount)
    counter("caffeine.request").withTags(Map("name" -> name, "hit" -> false)).increment(stats.missCount)
    histogram("caffeine.hit.rate").withTag("name", name).record((stats.hitRate * 100000).toLong)
    if (stats.totalLoadTime > 0) {
      counter("caffeine.load.count").withTags(Map("name" -> name, "success" -> "success")).increment(stats.loadSuccessCount)
      counter("caffeine.load.count").withTags(Map("name" -> name, "success" -> "failure")).increment(stats.loadFailureCount)
      timer("caffeine.loadTime.cumulated").withTag("name", name).record(stats.totalLoadTime / 1000000) // in millis; too much nanos for Kamon to handle)
      timer("caffeine.loadTime.penalty").withTag("name", name).record(stats.averageLoadPenalty.toLong)
    }
    counter("caffeine.eviction.count").withTag("name", name).increment(stats.evictionCount)
    gauge("caffeine.entry.count").withTag("name", name).update(cache.estimatedSize)
  }
  object evalCache {
    private val r = counter("evalCache.request")
    def request(ply: Int, isHit: Boolean) = r.withTags(Map("ply" -> ply.toLong, "hit" -> isHit))
    object upgrade {
      val count = counter("evalCache.upgrade.count").withoutTags
      val members = gauge("evalCache.upgrade.members").withoutTags
      val evals = gauge("evalCache.upgrade.evals").withoutTags
      val expirable = gauge("evalCache.upgrade.expirable").withoutTags
    }
  }
  object lobby {
    object hook {
      val create = counter("lobby.hook.create").withoutTags
      val join = counter("lobby.hook.join").withoutTags
      val size = histogram("lobby.hook.size").withoutTags
    }
    object seek {
      val create = counter("lobby.seek.create").withoutTags
      val join = counter("lobby.seek.join").withoutTags
    }
    object socket {
      val getSris = timer("lobby.socket.getSris").withoutTags
      val member = gauge("lobby.socket.member").withoutTags
      val idle = gauge("lobby.socket.idle").withoutTags
      val hookSubscribers = gauge("lobby.socket.hookSubscribers").withoutTags
    }
    object pool {
      object wave {
        def scheduled(id: String) = counter("lobby.pool.wave.scheduled").withTag("pool", id)
        def full(id: String) = counter("lobby.pool.wave.full").withTag("pool", id)
        def candidates(id: String) = histogram("lobby.pool.wave.candidates").withTag("pool", id)
        def paired(id: String) = histogram("lobby.pool.wave.paired").withTag("pool", id)
        def missed(id: String) = histogram("lobby.pool.wave.missed").withTag("pool", id)
        def wait(id: String) = histogram("lobby.pool.wave.wait").withTag("pool", id)
        def ratingDiff(id: String) = histogram("lobby.pool.wave.ratingDiff").withTag("pool", id)
        def withRange(id: String) = histogram("lobby.pool.wave.withRange").withTag("pool", id)
      }
      object thieve {
        def timeout(id: String) = counter("lobby.pool.thieve.timeout").withTag("pool", id)
        def candidates(id: String) = histogram("lobby.pool.thieve.candidates").withTag("pool", id)
        def stolen(id: String) = histogram("lobby.pool.thieve.stolen").withTag("pool", id)
      }
      object join {
        def count(id: String) = counter("lobby.pool.join.count").withTag("pool", id)
      }
      object leave {
        def count(id: String) = counter("lobby.pool.leave.count").withTag("pool", id)
        def wait(id: String) = histogram("lobby.pool.leave.wait").withTag("pool", id)
      }
      object matchMaking {
        def duration(id: String) = timer("lobby.pool.matchMaking.duration").withTag("pool", id)
      }
      object gameStart {
        def duration(id: String) = timer("lobby.pool.gameStart.duration").withTag("pool", id)
      }
    }
  }
  object rating {
    def distribution(perfKey: String, rating: Int) =
      gauge("rating.distribution").withTags(Map("perf" -> perfKey, "rating" -> rating.toLong))
    object regulator {
      def micropoints(perfKey: String) = histogram("rating.regulator").withTag("perf", perfKey)
    }
  }

  object round {
    object api {
      val player = timer("round.api").withTag("endpoint", "player")
      val watcher = timer("round.api").withTag("endpoint", "watcher")
      val embed = timer("round.api").withTag("endpoint", "embed")
    }
    object forecast {
      val create = counter("round.forecast.create").withoutTags
    }
    object move {
      object lag {
        val compDeviation = histogram("round.move.lag.comp_deviation").withoutTags
        def uncomped(key: String) = histogram("round.move.lag.uncomped_ms").withTag("key", key)
        def uncompStdDev(key: String) = histogram("round.move.lag.uncomp_stdev_ms").withTag("key", key)
        val stdDev = histogram("round.move.lag.stddev_ms").withoutTags
        val mean = histogram("round.move.lag.mean_ms").withoutTags
        val coefVar = histogram("round.move.lag.coef_var_1000").withoutTags
        val compEstStdErr = histogram("round.move.lag.comp_est_stderr_1000").withoutTags
        val compEstOverErr = histogram("round.move.lag.avg_over_error_ms").withoutTags
      }
      val time = timer("round.move.time").withoutTags
    }
    object error {
      val client = counter("round.error").withTag("from", "client")
      val fishnet = counter("round.error").withTag("from", "fishnet")
      val glicko = counter("round.error").withTag("from", "glicko")
    }
    object titivate {
      val time = future("round.titivate.time")
      val game = histogram("round.titivate.game").withoutTags // how many games were processed
      val total = histogram("round.titivate.total").withoutTags // how many games should have been processed
      val old = histogram("round.titivate.old").withoutTags // how many old games remain
    }
    object alarm {
      val time = timer("round.alarm.time").withoutTags
    }
    object expiration {
      val count = counter("round.expiration.count").withoutTags
    }
  }
  object playban {
    def outcome(out: String) = counter("playban.outcome").withTag("outcome", out)
    object ban {
      val count = counter("playban.ban.count").withoutTags
      val mins = counter("playban.ban.mins").withoutTags
    }
  }
  object explorer {
    object index {
      def count(success: Boolean) = counter("explorer.index.count").withTag("success", successTag(success))
      val time = timer("explorer.index.time").withoutTags
    }
  }
  object timeline {
    val notification = counter("timeline.notification").withoutTags
  }
  object insight {
    val request = future("insight.request.time")
    val index = future("insight.index.time")
  }
  object search {
    def time(op: String, index: String, success: Boolean) = timer("search.client.time").withTags(Map(
      "op" -> op, "index" -> index, "success" -> successTag(success)
    ))
  }
  object user {
    val online = gauge("user.online").withoutTags
    object register {
      def count(api: Option[ApiVersion]) = counter("user.register.count").withTag("api", apiTag(api))
      def mustConfirmEmail(v: String) = counter("user.register.mustConfirmEmail").withTag("type", v)
      def confirmEmailResult(success: Boolean) = counter("user.register.confirmEmail").withTag("success", successTag(success))
      val modConfirmEmail = counter("user.register.modConfirmEmail").withoutTags
    }
    object auth {
      val bcFullMigrate = counter("user.auth.bcFullMigrate").withoutTags
      val hashTime = timer("user.auth.hashTime").withoutTags
      def count(success: Boolean) = counter("user.auth.count").withTag("success", successTag(success))

      def passwordResetRequest(s: String) = counter("user.auth.passwordResetRequest").withTag("type", s)
      def passwordResetConfirm(s: String) = counter("user.auth.passwordResetConfirm").withTag("type", s)

      def magicLinkRequest(s: String) = counter("user.auth.magicLinkRequest").withTag("type", s)
      def magicLinkConfirm(s: String) = counter("user.auth.magicLinkConfirm").withTag("type", s)
    }
    object oauth {
      def request(success: Boolean) = counter("user.oauth.request").withTag("success", successTag(success))
    }
  }
  object trouper {
    def queueSize(name: String) = gauge("trouper.queueSize").withTag("name", name)
  }
  object mod {
    object report {
      val unprocessed = gauge("mod.report.unprocessed").withoutTags
      val close = counter("mod.report.close").withoutTags
      def create(reason: String) = counter("mod.report.create").withTag("reason", reason)
      def discard(reason: String) = counter("mod.report.discard").withTag("reason", reason)
    }
    object log {
      val create = counter("mod.log.create").withoutTags
    }
    object irwin {
      val report = counter("mod.report.irwin.report").withoutTags
      val mark = counter("mod.report.irwin.mark").withoutTags
      def ownerReport(name: String) = counter("mod.irwin.ownerReport").withTag("name", name)
      def streamEventType(name: String) = counter("mod.irwin.stream.eventType").withTag("name", name)
    }
  }
  object relay {
    val ongoing = gauge("relay.ongoing").withoutTags
    val moves = counter("relay.moves").withoutTags
    val syncTime = future("relay.sync.time")
  }
  object bot {
    def moves(username: String) = counter("bot.moves").withTag("name", username)
    def chats(username: String) = counter("bot.chats").withTag("name", username)
  }
  object cheat {
    val cssBot = counter("cheat.cssBot").withoutTags
    val holdAlert = counter("cheat.holdAlert").withoutTags
    object autoAnalysis {
      def reason(r: String) = counter("cheat.autoAnalysis").withTag("reason", r)
    }
    object autoMark {
      val count = counter("cheat.autoMark.count").withoutTags
    }
    object autoReport {
      val count = counter("cheat.autoReport.count").withoutTags
    }
  }
  object email {
    object send {
      private val c = counter("email.send")
      val resetPassword = c.withTag("type", "resetPassword")
      val magicLink = c.withTag("type", "magicLink")
      val fix = c.withTag("type", "fix")
      val change = c.withTag("type", "change")
      val confirmation = c.withTag("type", "confirmation")
      val time = future("email.send.time")
      val retry = counter("email.retry").withoutTags
      val timeout = counter("email.timeout").withoutTags
    }
    val disposableDomain = gauge("email.disposableDomain").withoutTags
  }
  object security {
    val torNodes = gauge("security.tor.node").withoutTags
    object firewall {
      val block = counter("security.firewall.block").withoutTags
      val ip = gauge("security.firewall.ip").withoutTags
      val prints = gauge("security.firewall.prints").withoutTags
    }
    object proxy {
      val request = future("security.proxy.time")
      val percent = histogram("security.proxy.percent").withoutTags
    }
    def rateLimit(key: String) = counter("security.rateLimit.count").withTag("key", key)
    def linearLimit(key: String) = counter("security.linearLimit.count").withTag("key", key)
    object dnsApi {
      val mx = future("security.dnsApi.mx.time")
    }
    object checkMailApi {
      def fetch(success: Boolean, block: Boolean) =
        timer("checkMail.fetch").withTags(Map("success" -> successTag(success), "block" -> block))
    }
    def usersAlikeTime(field: String) = timer("security.usersAlike.time").withTag("field", field)
    def usersAlikeFound(field: String) = histogram("security.usersAlike.found").withTag("field", field)
  }
  object tv {
    object streamer {
      def present(n: String) = gauge("tv.streamer.present").withTag("name", n)
    }
  }
  object relation {
    private val c = counter("relation.action")
    val follow = c.withTag("type", "follow")
    val unfollow = c.withTag("type", "unfollow")
    val block = c.withTag("type", "block")
    val unblock = c.withTag("type", "unblock")
  }
  object coach {
    object pageView {
      def profile(coachId: String) = counter("coach.pageView").withTag("name", coachId)
    }
  }
  object tournament {
    object pairing {
      val count = counter("tournament.pairing.count").withoutTags
      val create = future("tournament.pairing.create")
      val prep = future("tournament.pairing.prep")
    }
    val created = gauge("tournament.count").withTag("type", "created")
    val started = gauge("tournament.count").withTag("type", "started")
    val waitingPlayers = histogram("tournament.waitingPlayers").withoutTags
    object startedOrganizer {
      val tick = future("tournament.startedOrganizer.tick")
      val waitingUsers = future("tournament.startedOrganizer.waitingUsers")
    }
    object createdOrganizer {
      val tick = future("tournament.createdOrganizer.tick")
    }
    def apiShowPartial(partial: Boolean, client: String)(success: Boolean) =
      timer("tournament.api.show").withTags(Map(
        "partial" -> partial, "success" -> successTag(success), "client" -> client
      ))
  }
  object plan {
    object amount {
      val paypal = counter("plan.amount").withTag("service", "paypal")
      val stripe = counter("plan.amount").withTag("service", "stripe")
    }
    object count {
      val paypal = counter("plan.count").withTag("service", "paypal")
      val stripe = counter("plan.count").withTag("service", "stripe")
    }
    val goal = gauge("plan.goal").withoutTags
    val current = gauge("plan.current").withoutTags
    val percent = gauge("plan.percent").withoutTags
  }
  object forum {
    object post {
      val create = counter("forum.post.create").withoutTags
    }
    object topic {
      val view = counter("forum.topic.view").withoutTags
    }
  }
  object puzzle {
    object selector {
      val time = timer("puzzle.selector.time").withoutTags
      val vote = histogram("puzzle.selector.vote").withoutTags
    }
    object batch {
      object selector {
        val count = counter("puzzle.batch.selector.count").withoutTags
        val time = timer("puzzle.batch.selector").withoutTags
      }
      val solve = counter("puzzle.batch.solve").withoutTags
    }
    object round {
      def attempt(mate: Boolean, user: Boolean, endpoint: String) =
        counter("puzzle.attempt.count").withTags(Map("mate" -> mate, "user" -> user, "endpoint" -> endpoint))
    }
    object vote {
      val up = counter("puzzle.vote.count").withTag("dir", "up")
      val down = counter("puzzle.vote.count").withTag("dir", "down")
    }
    val crazyGlicko = counter("puzzle.crazyGlicko").withoutTags
  }
  object game {
    def finish(variant: String, speed: String, source: String, mode: String, status: String) =
      counter("game.finish").withTags(Map(
        "variant" -> variant, "speed" -> speed, "source" -> source, "mode" -> mode, "status" -> status
      ))
    val fetch = counter("game.fetch.count").withoutTags
    val fetchLight = counter("game.fetchLight.count").withoutTags
    val loadClockHistory = counter("game.loadClockHistory.count").withoutTags
    object pgn {
      def encode(format: String) = timer("game.pgn.encode").withTag("format", format)
      def decode(format: String) = timer("game.pgn.decode").withTag("format", format)
    }
    val idCollision = counter("game.idCollision").withoutTags
  }
  object chat {
    def message(troll: Boolean) = counter("chat.message").withTag("troll", troll)
  }
  object push {
    object register {
      def in(platform: String) = counter("push.register").withTag("platform", platform)
      val out = counter("push.register.out").withoutTags
    }
    object send {
      private def send(tpe: String)(platform: String): Unit = counter("push.send").withTags(Map(
        "type" -> tpe, "platform" -> platform
      ))
      val move = send("move") _
      val takeback = send("takeback") _
      val corresAlarm = send("corresAlarm") _
      val finish = send("finish") _
      val message = send("message") _
      object challenge {
        val create = send("challengeCreate") _
        val accept = send("challengeAccept") _
      }
    }
    val googleTokenTime = timer("push.send.googleToken").withoutTags
  }
  object fishnet {
    object client {
      def result(client: String) = new {
        private val c = counter("fishnet.client.result")
        private def apply(r: String): Counter = c.withTags(Map("client" -> client, "result" -> r))
        val success = apply("success")
        val failure = apply("failure")
        val weak = apply("weak")
        val timeout = apply("timeout")
        val notFound = apply("notFound")
        val notAcquired = apply("notAcquired")
        val abort = apply("abort")
      }
      def status(enabled: Boolean) = gauge("fishnet.client.status").withTag("enabled", enabled)
      def version(v: String) = gauge("fishnet.client.version").withTag("version", v)
      def stockfish(v: String) = gauge("fishnet.client.engine.stockfish").withTag("version", v)
      def python(v: String) = gauge("fishnet.client.python").withTag("version", v)
    }
    val queue = timer("fishnet.queue.db").withoutTags
    val acquire = future("fishnet.acquire")
    object work {
      val acquired = gauge("fishnet.work").withTag("type", "acquired")
      val queued = gauge("fishnet.work").withTag("type", "queued")
      val forUser = gauge("fishnet.work.forUser").withoutTags
    }
    object analysis {
      def by(client: String) = new {
        val hash = histogram("fishnet.analysis.hash").withTag("client", client)
        val threads = gauge("fishnet.analysis.threads").withTag("client", client)
        val movetime = histogram("fishnet.analysis.movetime").withTag("client", client)
        val node = histogram("fishnet.analysis.node").withTag("client", client)
        val nps = histogram("fishnet.analysis.nps").withTag("client", client)
        val depth = histogram("fishnet.analysis.depth").withTag("client", client)
        val pvSize = histogram("fishnet.analysis.pvSize").withTag("client", client)
        def pv(isLong: Boolean) = counter("fishnet.analysis.pvs").withTags(Map("client" -> client, "long" -> isLong))
        val totalMeganode = counter("fishnet.analysis.total.meganode").withTag("client", client)
        val totalSecond = counter("fishnet.analysis.total.second").withTag("client", client)
        val totalPosition = counter("fishnet.analysis.total.position").withTag("client", client)
      }
      val post = timer("fishnet.analysis.post").withoutTags
      def requestCount(tpe: String) = counter("fishnet.analysis.request").withTag("type", tpe)
      val evalCacheHits = counter("fishnet.analysis.evalCacheHits").withoutTags
    }
    object http {
      def request(hit: Boolean) = counter("fishnet.http.acquire").withTag("hit", hit)
    }
  }
  object api {
    val userGames = counter("api.cost").withTag("endpoint", "userGames")
    val users = counter("api.cost").withTag("endpoint", "users")
    val game = counter("api.cost").withTag("endpoint", "game")
    val activity = counter("api.cost").withTag("endpoint", "activity")
  }
  object export {
    object pgn {
      val game = counter("export.pgn").withTag("type", "game")
      val study = counter("export.pgn").withTag("type", "study")
      val studyChapter = counter("export.pgn").withTag("type", "studyChapter")
    }
    object png {
      val game = counter("export.png").withTag("type", "game")
      val puzzle = counter("export.png").withTag("type", "puzzle")
    }
  }
  object bus {
    val classifiers = gauge("bus.classifiers").withoutTags
  }

  type TimerPath = lila.mon.type => Timer
  type CounterPath = lila.mon.type => Counter

  private def future(name: String) = (success: Boolean) =>
    timer(name).withTag("success", successTag(success))

  private def successTag(success: Boolean) = if (success) "success" else "failure"

  private def apiTag(api: Option[ApiVersion]) = api.fold("-")(_.toString)

  implicit def mapToTags(m: Map[String, Any]): TagSet = TagSet from m
}
