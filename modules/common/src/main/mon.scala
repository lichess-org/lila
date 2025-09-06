package lila

import com.github.benmanes.caffeine.cache.Cache as CaffeineCache
import kamon.metric.{ Counter, Timer }
import kamon.tag.TagSet
import chess.variant.Variant

import lila.core.id.*
import lila.core.net.*
import lila.core.userId.{ UserId, UserName }
import lila.core.perf.PerfKey

object mon:

  import kamon.Kamon.{ timer, gauge, counter, histogram }

  // https://github.com/kamon-io/Kamon/issues/752
  extension (s: String)
    def escape: String =
      val builder = java.lang.StringBuilder(s.length)
      for c <- s.toCharArray do
        if c != '"' & c != '\n' && c != '\\'
        then builder.append(c)
      builder.toString

  private def tags(elems: (String, Any)*): Map[String, Any] = Map.from(elems)

  object http:
    private val reqTime = timer("http.time")
    private val reqCount = counter("http.count")
    private val mobCount = counter("http.mobile.count")

    def time(action: String) = reqTime.withTag("action", action)

    def count(action: String, client: String, method: String, code: Int) =
      reqCount.withTags:
        tags("action" -> action, "client" -> client, "method" -> method, "code" -> code.toLong)

    def errorCount(action: String, client: String, method: String, code: Int) =
      counter("http.error").withTags:
        tags("action" -> action, "client" -> client, "method" -> method, "code" -> code.toLong)

    def mobileCount(action: String, version: String, auth: Boolean, os: String) =
      mobCount.withTags:
        tags(
          "action" -> action,
          "version" -> version,
          "auth" -> (if auth then "auth" else "anon"),
          "os" -> os
        )

    def path(p: String) = counter("http.path.count").withTag("path", p.escape)
    val userGamesCost = counter("http.userGames.cost").withoutTags()
    def csrfError(tpe: String, action: String, client: String) =
      counter("http.csrf.error").withTags(tags("type" -> tpe, "action" -> action, "client" -> client))
    val fingerPrint = timer("http.fingerPrint.time").withoutTags()
  object syncache:
    def miss(name: String) = counter("syncache.miss").withTag("name", name)
    def timeout(name: String) = counter("syncache.timeout").withTag("name", name)
    def compute(name: String) = timer("syncache.compute").withTag("name", name)
    def wait(name: String) = timer("syncache.wait").withTag("name", name)
  def caffeineStats(cache: CaffeineCache[?, ?], name: String): Unit =
    val stats = cache.stats
    gauge("caffeine.request").withTags(tags("name" -> name, "hit" -> true)).update(stats.hitCount.toDouble)
    gauge("caffeine.request").withTags(tags("name" -> name, "hit" -> false)).update(stats.missCount.toDouble)
    histogram("caffeine.hit.rate").withTag("name", name).record((stats.hitRate * 100000).toLong)
    if stats.totalLoadTime > 0 then
      gauge("caffeine.load.count")
        .withTags(tags("name" -> name, "success" -> "success"))
        .update(stats.loadSuccessCount.toDouble)
      gauge("caffeine.load.count")
        .withTags(tags("name" -> name, "success" -> "failure"))
        .update(stats.loadFailureCount.toDouble)
      gauge("caffeine.loadTime.cumulated")
        .withTag("name", name)
        .update(stats.totalLoadTime / 1000000d) // in millis; too much nanos for Kamon to handle)
      timer("caffeine.loadTime.penalty").withTag("name", name).record(stats.averageLoadPenalty.toLong)
    gauge("caffeine.eviction.count").withTag("name", name).update(stats.evictionCount.toDouble)
    gauge("caffeine.entry.count").withTag("name", name).update(cache.estimatedSize.toDouble)
    ()
  object mongoCache:
    def request(name: String, hit: Boolean) =
      counter("mongocache.request").withTags:
        tags(
          "name" -> name,
          "hit" -> hit
        )
    def compute(name: String) = timer("mongocache.compute").withTag("name", name)
  object evalCache:
    private val r = counter("evalCache.request")
    def request(ply: Int, isHit: Boolean) =
      r.withTags(tags("ply" -> (if ply < 15 then ply.toString else "15+"), "hit" -> isHit))
    object upgrade:
      val count = counter("evalCache.upgrade.count").withoutTags()
      val members = gauge("evalCache.upgrade.members").withoutTags()
      val evals = gauge("evalCache.upgrade.evals").withoutTags()
      val expirable = gauge("evalCache.upgrade.expirable").withoutTags()
  object lobby:
    object hook:
      val create = counter("lobby.hook.create").withoutTags()
      val join = counter("lobby.hook.join").withoutTags()
      val size = histogram("lobby.hook.size").withoutTags()
    object seek:
      val create = counter("lobby.seek.create").withoutTags()
      val join = counter("lobby.seek.join").withoutTags()
    object socket:
      val getSris = timer("lobby.socket.getSris").withoutTags()
      val member = gauge("lobby.socket.member").withoutTags()
      val idle = gauge("lobby.socket.idle").withoutTags()
      val hookSubscribers = gauge("lobby.socket.hookSubscribers").withoutTags()
    object pool:
      object wave:
        def scheduled(id: String) = counter("lobby.pool.wave.scheduled").withTag("pool", id)
        def full(id: String) = counter("lobby.pool.wave.full").withTag("pool", id)
        def candidates(id: String) = histogram("lobby.pool.wave.candidates").withTag("pool", id)
        def paired(id: String) = histogram("lobby.pool.wave.paired").withTag("pool", id)
        def missed(id: String) = histogram("lobby.pool.wave.missed").withTag("pool", id)
        def ratingDiff(id: String) = histogram("lobby.pool.wave.ratingDiff").withTag("pool", id)
        def withRange(id: String) = histogram("lobby.pool.wave.withRange").withTag("pool", id)
      object thieve:
        def stolen(id: String) = histogram("lobby.pool.thieve.stolen").withTag("pool", id)
    private val lobbySegment = timer("lobby.segment")
    def segment(seg: String) = lobbySegment.withTag("segment", seg)
  object rating:
    def distribution(perfKey: PerfKey, rating: Int) =
      gauge("rating.distribution").withTags(tags("perf" -> perfKey, "rating" -> rating.toLong))
    object regulator:
      def micropoints(perfKey: PerfKey) = histogram("rating.regulator").withTag("perf", perfKey.value)
  object perfStat:
    def indexTime = timer("perfStat.indexTime").withoutTags()

  object round:
    object api:
      val player = timer("round.api").withTag("endpoint", "player")
      val watcher = timer("round.api").withTag("endpoint", "watcher")
    object forecast:
      val create = counter("round.forecast.create").withoutTags()
    object move:
      object lag:
        val compDeviation = histogram("round.move.lag.comp_deviation").withoutTags()
        def uncomped(key: String) = histogram("round.move.lag.uncomped_ms").withTag("key", key)
        def uncompStdDev(key: String) = histogram("round.move.lag.uncomp_stdev_ms").withTag("key", key)
        val stdDev = histogram("round.move.lag.stddev_ms").withoutTags()
        val mean = histogram("round.move.lag.mean_ms").withoutTags()
        val coefVar = histogram("round.move.lag.coef_var_1000").withoutTags()
        val compEstStdErr = histogram("round.move.lag.comp_est_stderr_1000").withoutTags()
        val compEstOverErr = histogram("round.move.lag.avg_over_error_ms").withoutTags()
        val moveComp = timer("round.move.lag.comped").withoutTags()
      val time = timer("round.move.time").withoutTags()
    object error:
      val client = counter("round.error").withTag("from", "client")
      val fishnet = counter("round.error").withTag("from", "fishnet")
      val glicko = counter("round.error").withTag("from", "glicko")
      val other = counter("round.error").withTag("from", "other")
    object titivate:
      val time = future("round.titivate.time")
      val game = histogram("round.titivate.game").withoutTags() // how many games were processed
      val total = histogram("round.titivate.total").withoutTags() // how many games should have been processed
      val old = histogram("round.titivate.old").withoutTags() // how many old games remain
      def broken(error: String) = counter("round.titivate.broken").withTag("error", error) // broken game
    object alarm:
      val time = timer("round.alarm.time").withoutTags()
    object expiration:
      val count = counter("round.expiration.count").withoutTags()
    val asyncActorCount = gauge("round.asyncActor.count").withoutTags()
    object correspondenceEmail:
      val emails = histogram("round.correspondenceEmail.emails").withoutTags()
      val time = future("round.correspondenceEmail.time")
    object farming:
      val bot = counter("round.farming.bot").withoutTags()
      val provisional = counter("round.farming.provisional").withoutTags()
  object playban:
    def outcome(out: String) = counter("playban.outcome").withTag("outcome", out)
    object ban:
      val count = counter("playban.ban.count").withoutTags()
      val mins = histogram("playban.ban.mins").withoutTags()
  object explorer:
    object index:
      def count(success: Boolean) = counter("explorer.index.count").withTag("success", successTag(success))
      val time = timer("explorer.index.time").withoutTags()
  object timeline:
    val notification = counter("timeline.notification").withoutTags()
  object insight:
    val user = future("insight.request.time", "user")
    val peers = future("insight.request.time", "peer")
    val index = future("insight.index.time")
  object tutor:
    def buildSegment(segment: String) = future("tutor.build.segment", segment)
    def buildFull = future("tutor.build.full")
    def askMine = askAs("mine")
    def askPeer = askAs("peer")
    def buildTimeout = counter("tutor.build.timeout").withoutTags()
    def peerMatch(hit: Boolean) = counter("tutor.peerMatch").withTag("hit", hitTag(hit))
    def parallelism = gauge("tutor.build.parallelism").withoutTags()
    private def askAs(as: "mine" | "peer")(question: String, perf: PerfKey | "all") =
      future("tutor.insight.ask", tags("question" -> question, "perf" -> perf, "as" -> as))
  object search:
    def time(op: "search" | "count", index: String, success: Boolean) =
      timer("search.client.time").withTags:
        tags(
          "op" -> op,
          "index" -> index,
          "success" -> successTag(success)
        )
  object asyncActor:
    def overflow(name: String) = counter("asyncActor.overflow").withTag("name", name)
    def queueSize(name: String) = histogram("asyncActor.queueSize").withTag("name", name)
  object irc:
    object zulip:
      def say(stream: String) = future("irc.zulip.say", tags("stream" -> stream.escape))
  object user:
    val online = gauge("user.online").withoutTags()
    object register:
      def count(
          emailDomain: Option[Domain],
          confirm: String,
          captcha: String,
          ipSusp: Boolean,
          fp: Boolean,
          proxy: Option[String],
          country: String,
          dispAttempts: Int,
          api: Option[ApiVersion]
      ) =
        counter("user.register.count").withTags:
          tags(
            "email" -> emailDomain.fold("?")(_.value),
            "confirm" -> confirm,
            "captcha" -> captcha,
            "ipSusp" -> ipSusp,
            "fp" -> fp,
            "proxy" -> proxy.getOrElse("no"),
            "country" -> country.escape,
            "dispAttempts" -> dispAttempts,
            "api" -> apiTag(api)
          )
      def mustConfirmEmail(v: String) = counter("user.register.mustConfirmEmail").withTag("type", v)
      def confirmEmailResult(success: Boolean) =
        counter("user.register.confirmEmail").withTag("success", successTag(success))
      val modConfirmEmail = counter("user.register.modConfirmEmail").withoutTags()
    object auth:
      val bcFullMigrate = counter("user.auth.bcFullMigrate").withoutTags()
      val hashTime = timer("user.auth.hashTime").withoutTags()
      def count(success: Boolean) = counter("user.auth.count").withTag("success", successTag(success))

      def passwordResetRequest(s: String) = counter("user.auth.passwordResetRequest").withTag("type", s)
      def passwordResetConfirm(s: String) = counter("user.auth.passwordResetConfirm").withTag("type", s)

      def reopenRequest(s: String) = counter("user.auth.reopenRequest").withTag("type", s)
      def reopenConfirm(s: String) = counter("user.auth.reopenConfirm").withTag("type", s)
    object oauth:
      def request(success: Boolean) = counter("user.oauth.request").withTags:
        tags("success" -> successTag(success))
    private val userSegment = timer("user.segment")
    def segment(seg: String) = userSegment.withTag("segment", seg)
    def leaderboardCompute = future("user.leaderboard.compute")
  object actor:
    def queueSize(name: String) = gauge("trouper.queueSize").withTag("name", name)
  object mod:
    object report:
      val highest = gauge("mod.report.highest").withoutTags()
      val close = counter("mod.report.close").withoutTags()
      def create(reason: String, score: Int) =
        counter("mod.report.create").withTags:
          tags("reason" -> reason, "score" -> score)
      object automod:
        val request = future("mod.report.automod.request")
        def assessment(a: String) = counter("mod.report.automod.assessment").withTag("assessment", a)
    object log:
      val create = counter("mod.log.create").withoutTags()
    object irwin:
      val report = counter("mod.report.irwin.report").withoutTags()
      val mark = counter("mod.report.irwin.mark").withoutTags()
      def ownerReport(name: String) = counter("mod.irwin.ownerReport").withTag("name", name)
      def streamEventType(name: String) = counter("mod.irwin.stream.eventType").withTag("name", name)
    object kaladin:
      def request(by: String) = counter("mod.kaladin.request").withTag("by", by)
      def insufficientMoves(by: String) = counter("mod.kaladin.insufficientMoves").withTag("by", by)
      def queue(priority: Int) = gauge("mod.kaladin.queue").withTag("priority", priority)
      def error(errKind: String) = counter("mod.kaladin.error").withTag("error", errKind)
      val activation = histogram("mod.report.kaladin.activation").withoutTags()
      val report = counter("mod.report.kaladin.report").withoutTags()
      val mark = counter("mod.report.kaladin.mark").withoutTags()
    object comm:
      def segment(seg: String) = timer("mod.comm.segmentLat").withTag("segment", seg)
    def zoneSegment(name: String) = future("mod.zone.segment", name)
  object relay:
    private def by(official: Boolean) = if official then "official" else "user"
    private def relay(official: Boolean, id: RelayTourId, slug: String) =
      tags("by" -> by(official), "slug" -> s"$slug/$id")
    def ongoing(official: Boolean) = gauge("relay.ongoing").withTag("by", by(official))
    val crowdMonitor = gauge("relay.crowdMonitor").withoutTags()
    def moves(official: Boolean, id: RelayTourId, slug: String) =
      counter("relay.moves").withTags(relay(official, id, slug))
    def fetchTime(official: Boolean, id: RelayTourId, slug: String) =
      timer("relay.fetch.time").withTags(relay(official, id, slug))
    def syncTime(official: Boolean, id: RelayTourId, slug: String) =
      timer("relay.sync.time").withTags(relay(official, id, slug))
    def httpGet(code: Int, host: String, etag: String, proxy: Option[String]) =
      timer("relay.http.get").withTags:
        tags(
          "code" -> code.toLong,
          "host" -> host.escape,
          "etag" -> etag.escape,
          "proxy" -> proxy.getOrElse("none")
        )
    val dedup = counter("relay.fetch.dedup").withoutTags()
    def push(name: String, user: UserName, client: String)(moves: Int, errors: Int) =
      val ts = tags("name" -> name.escape, "user" -> user, "client" -> client.escape)
      histogram("relay.push.moves").withTags(ts).record(moves)
      histogram("relay.push.errors").withTags(ts).record(errors)

  object bot:
    def moves(username: String) = counter("bot.moves").withTag("name", username)
    def chats(username: String) = counter("bot.chats").withTag("name", username)
    def gameStream(event: "start" | "stop") = counter("bot.gameStream").withTag("event", event)
  object cheat:
    def selfReport(wildName: String, auth: Boolean) =
      val name = if wildName.startsWith("soc: ") then "soc" else wildName.takeWhile(' ' !=)
      counter("cheat.selfReport").withTags(tags("name" -> name.escape, "auth" -> auth))
    val holdAlert = counter("cheat.holdAlert").withoutTags()
    def autoAnalysis(reason: String) = counter("cheat.autoAnalysis").withTag("reason", reason)
    val autoMark = counter("cheat.autoMark.count").withoutTags()
    val autoReport = counter("cheat.autoReport.count").withoutTags()
  object email:
    object send:
      private val c = counter("email.send")
      val resetPassword = c.withTag("type", "resetPassword")
      val magicLink = c.withTag("type", "magicLink")
      val reopen = c.withTag("type", "reopen")
      val fix = c.withTag("type", "fix")
      val change = c.withTag("type", "change")
      val confirmation = c.withTag("type", "confirmation")
      val welcome = c.withTag("type", "welcome")
      val time = future("email.send.time")
    val disposableDomain = gauge("email.disposableDomain").withoutTags()
  object security:
    val torNodes = gauge("security.tor.node").withoutTags()
    object firewall:
      val block = counter("security.firewall.block").withoutTags()
      val ip = gauge("security.firewall.ip").withoutTags()
      val prints = gauge("security.firewall.prints").withoutTags()
    object proxy:
      val request = future("security.proxy.time")
      def result(r: Option[String]) = counter("security.proxy.result").withTag("result", r.getOrElse("none"))
      def hit(prox: String, action: String) =
        counter("security.proxy.hit").withTags(tags("proxy" -> prox, "action" -> action))
    def rateLimit(key: String) = counter("security.rateLimit.count").withTag("key", key)
    def concurrencyLimit(key: String) = counter("security.concurrencyLimit.count").withTag("key", key)
    object dnsApi:
      val mx = future("security.dnsApi.mx.time")
    object verifyMailApi:
      def fetch(success: Boolean, ok: Boolean) =
        timer("verifyMail.fetch").withTags(tags("success" -> successTag(success), "ok" -> ok))
    object mailcheckApi:
      def fetch(success: Boolean, ok: Boolean) =
        timer("mailcheck.fetch").withTags(tags("success" -> successTag(success), "ok" -> ok))
    object hCaptcha:
      def hit(client: String, result: String) =
        counter("hcaptcha.hit").withTags(tags("client" -> client, "result" -> result))
      def form(client: String, result: String) =
        counter("hcaptcha.form").withTags(tags("client" -> client, "result" -> result))
    object pwned:
      def get(res: Boolean) = timer("security.pwned.result").withTag("res", res)
    object login:
      def attempt(byEmail: Boolean, stuffing: String, pwned: Boolean, result: Boolean) =
        counter("security.login.attempt").withTags:
          tags(
            "by" -> (if byEmail then "email" else "name"),
            "stuffing" -> stuffing,
            "pwned" -> pwned,
            "result" -> result
          )
      def proxy(tpe: String) = counter("security.login.proxy").withTag("proxy", tpe)
    def secretScanning(tokenType: String, source: String, hit: Boolean) =
      counter("security.githubSecretScanning.hit").withTags(
        tags("type" -> tokenType, "source" -> source.escape, "hit" -> hit)
      )
    def userTrust(trust: Boolean, cause: String) =
      counter("security.userTrust").withTags(tags("trust" -> trust, "cause" -> cause)).increment()
  object shutup:
    def analyzer = timer("shutup.analyzer.time").withoutTags()
  object tv:
    object selector:
      def candidates(channel: String) = histogram("tv.selector.candidates").withTag("channel", channel)
      def cheats(channel: String) = histogram("tv.selector.cheats").withTag("channel", channel)
      def rating(channel: String) = histogram("tv.selector.rating").withTag("channel", channel)
    object streamer:
      def present(n: String) = gauge("tv.streamer.present").withTag("name", n.escape)
      def twitch = future("tv.streamer.twitch")
  object relation:
    private val c = counter("relation.action")
    val follow = c.withTag("type", "follow")
    val unfollow = c.withTag("type", "unfollow")
    val block = c.withTag("type", "block")
    val unblock = c.withTag("type", "unblock")
  object clas:
    object student:
      def create(teacher: String) = counter("clas.student.create").withTag("teacher", teacher)
      def invite(teacher: String) = counter("clas.student.invite").withTag("teacher", teacher)
      object bloomFilter:
        val count = gauge("clas.student.bloomFilter.count").withoutTags()
        val fu = future("clas.student.bloomFilter.future")
  object tournament:
    object pairing:
      val batchSize = histogram("tournament.pairing.batchSize").withoutTags()
      val create = future("tournament.pairing.create")
      val createRanking = timer("tournament.pairing.create.ranking").withoutTags()
      val createPairings = timer("tournament.pairing.create.pairings").withoutTags()
      val createPlayerMap = timer("tournament.pairing.create.playerMap").withoutTags()
      val createInserts = timer("tournament.pairing.create.inserts").withoutTags()
      val createFeature = timer("tournament.pairing.create.feature").withoutTags()
      val createAutoPairing = timer("tournament.pairing.create.autoPairing").withoutTags()
      val prep = future("tournament.pairing.prep")
      val wmmatching = timer("tournament.pairing.wmmatching").withoutTags()
    val created = gauge("tournament.count").withTag("type", "created")
    val started = gauge("tournament.count").withTag("type", "started")
    val waitingPlayers = histogram("tournament.waitingPlayers").withoutTags()
    object startedOrganizer:
      val tick = future("tournament.startedOrganizer.tick")
      val waitingUsers = future("tournament.startedOrganizer.waitingUsers")
    object createdOrganizer:
      val tick = future("tournament.createdOrganizer.tick")
    object lilaHttp:
      val tick = future("tournament.lilaHttp.tick")
      val fullSize = histogram("tournament.lilaHttp.fullSize").withoutTags()
      val nbTours = gauge("tournament.lilaHttp.nbTours").withoutTags()
    def apiShowPartial(partial: Boolean, client: String)(success: Boolean) =
      timer("tournament.api.show").withTags:
        tags(
          "partial" -> partial,
          "success" -> successTag(success),
          "client" -> client
        )
    def withdrawableIds(reason: String) = future("tournament.withdrawableIds", reason)
    def action(tourId: String, action: String) =
      timer("tournament.api.action").withTags(tags("tourId" -> tourId, "action" -> action))
    object notifier:
      def tournaments = counter("tournament.notify.tournaments").withoutTags()
      def players = counter("tournament.notify.players").withoutTags()
    object featuring:
      def forTeams(page: "index" | "homepage") = future("tournament.featuring.forTeams", page)
  object swiss:
    val tick = future("swiss.tick")
    val bbpairing = timer("swiss.bbpairing").withoutTags()
    val scoringGet = future("swiss.scoring.get")
    val scoringRecompute = future("swiss.scoring.recompute")
    val startRound = future("swiss.director.startRound")
    def games(status: String) = histogram("swiss.ongoingGames").withTag("status", status)
    val json = future("swiss.json")
  object plan:
    object paypalLegacy:
      val amount = histogram("plan.amount").withTag("service", "paypal")
    object paypalCheckout:
      val amount = histogram("plan.amount").withTag("service", "paypalCheckout")
      val fetchAccessToken = future("plan.paypal.accessToken")
    val stripe = histogram("plan.amount").withTag("service", "stripe")
    val goal = gauge("plan.goal").withoutTags()
    val current = gauge("plan.current").withoutTags()
    val percent = gauge("plan.percent").withoutTags()
    def webhook(service: String, tpe: String) =
      counter("plan.webhook").withTags(tags("service" -> service, "tpe" -> tpe))
    object charge:
      def first(service: String) = counter("plan.charge.first").withTag("service", service)
      def countryCents(country: String, currency: java.util.Currency, service: String, gift: Boolean) =
        histogram("plan.charge.country.cents").withTags:
          tags(
            "country" -> country.escape,
            "currency" -> currency.getCurrencyCode,
            "service" -> service,
            "gift" -> gift
          )
  object forum:
    object post:
      val create = counter("forum.post.create").withoutTags()
    object topic:
      val view = counter("forum.topic.view").withoutTags()
    def reaction(r: String) = counter("forum.reaction").withTag("reaction", r)
  object msg:
    def post(verdict: String, isNew: Boolean, multi: Boolean) = counter("msg.post").withTags(
      tags("verdict" -> verdict, "isNew" -> isNew, "multi" -> multi)
    )
    val teamBulk = histogram("msg.bulk.team").withoutTags()
    def clasBulk(clasId: ClasId) = histogram("msg.bulk.clas").withTag("id", clasId.value)
  object puzzle:
    object selector:
      object user:
        def time(categ: String) = timer("puzzle.selector.user.puzzle").withTag("categ", categ)
        def retries(categ: String) = histogram("puzzle.selector.user.retries").withTag("categ", categ)
        val vote = histogram("puzzle.selector.user.vote").withoutTags()
        def tier(t: String, categ: String, difficulty: String) =
          counter("puzzle.selector.user.tier").withTags:
            tags("tier" -> t, "categ" -> categ, "difficulty" -> difficulty)
        def batch(nb: Int) = timer("puzzle.selector.user.batch").withTag("nb", nb)
      object anon:
        val time = timer("puzzle.selector.anon.puzzle").withoutTags()
        def batch(nb: Int) = timer("puzzle.selector.anon.batch").withTag("nb", nb)
        val vote = histogram("puzzle.selector.anon.vote").withoutTags()
      def nextPuzzleResult(result: String) =
        timer("puzzle.selector.user.puzzleResult").withTag("result", result)
    def nextPathFor(categ: String, requester: String) =
      timer("puzzle.path.nextFor").withTags(tags("categ" -> categ, "requester" -> requester))

    object batch:
      object selector:
        val count = counter("puzzle.batch.selector.count").withoutTags()
        val time = timer("puzzle.batch.selector").withoutTags()
      val solve = counter("puzzle.batch.solve").withoutTags()
    object round:
      def attempt(user: Boolean, theme: String, rated: Boolean) =
        counter("puzzle.attempt.count").withTags(tags("user" -> user, "theme" -> theme, "rated" -> rated))
    object vote:
      def count(up: Boolean, win: Boolean) =
        counter("puzzle.vote.count").withTags:
          tags(
            "up" -> up,
            "win" -> win
          )
      def theme(key: String, up: Option[Boolean], win: Boolean) =
        counter("puzzle.vote.theme").withTags:
          tags(
            "up" -> up.fold("cancel")(_.toString),
            "theme" -> key,
            "win" -> win
          )
      val future = mon.future("puzzle.vote.future")
    val crazyGlicko = counter("puzzle.crazyGlicko").withoutTags()
  object storm:
    object selector:
      val time = timer("storm.selector.time").withoutTags()
      val count = histogram("storm.selector.count").withoutTags()
      val rating = histogram("storm.selector.rating").withoutTags()
      def ratingSlice(index: Int) = histogram("storm.selector.ratingSlice").withTag("index", index)
    object run:
      def score(auth: Boolean) = histogram("storm.run.score").withTag("auth", auth)
      def sign(cause: String) = counter("storm.run.sign").withTag("cause", cause)
  object racer:
    private def tpe(lobby: Boolean) = if lobby then "lobby" else "friend"
    def race(lobby: Boolean) = counter("racer.lobby.race").withTag("tpe", tpe(lobby))
    def players(lobby: Boolean) =
      histogram("racer.lobby.players").withTag("tpe", tpe(lobby))
    def score(lobby: Boolean, auth: Boolean) =
      histogram("racer.player.score").withTags:
        tags(
          "tpe" -> tpe(lobby),
          "auth" -> auth
        )
  object streak:
    object selector:
      val time = timer("streak.selector.time").withoutTags()
      val count = histogram("streak.selector.count").withoutTags()
      val rating = histogram("streak.selector.rating").withoutTags()
      def ratingSlice(index: Int) = histogram("streak.selector.ratingSlice").withTag("index", index)
    object run:
      def score(auth: String) = histogram("streak.run.score").withTag("auth", auth)
  object game:
    import chess.{ Speed, Rated, Status }
    import lila.core.game.Source
    def finish(variant: Variant, speed: Speed, source: Option[Source], mode: Rated, status: Status) =
      counter("game.finish").withTags:
        tags(
          "variant" -> variant.key,
          "speed" -> speed,
          "source" -> source.fold("unknown")(_.name),
          "mode" -> mode.name,
          "status" -> status.name
        )
    val fetch = counter("game.fetch.count").withoutTags()
    val loadClockHistory = counter("game.loadClockHistory.count").withoutTags()
    object pgn:
      def encode(format: String) = timer("game.pgn.encode").withTag("format", format)
      def decode(format: String) = timer("game.pgn.decode").withTag("format", format)
    val idCollision = counter("game.idCollision").withoutTags()
    def idGenerator(collisions: Int) = timer("game.idGenerator").withTags(tags("collisions" -> collisions))
  object chat:
    private val msgCounter = counter("chat.message")
    def message(parent: String, troll: Boolean) =
      msgCounter.withTags(tags("parent" -> parent, "troll" -> troll))
    def fetch(parent: String) = timer("chat.fetch").withTag("parent", parent)
  object push:
    object register:
      def in(platform: String) = counter("push.register").withTag("platform", platform)
      val out = counter("push.register.out").withoutTags()
    object send:
      private def send(tpe: String)(platform: String, success: Boolean, count: Int): Unit =
        counter("push.send")
          .withTags:
            tags(
              "type" -> tpe,
              "platform" -> platform,
              "success" -> successTag(success)
            )
          .increment(count)
        ()
      val move = send("move")
      val takeback = send("takeback")
      val draw = send("draw")
      val corresAlarm = send("corresAlarm")
      val finish = send("finish")
      val message = send("message")
      val tourSoon = send("tourSoon")
      val forumMention = send("forumMention")
      val invitedStudy = send("invitedStudy")
      val streamStart = send("streamStart")
      val broadcastRound = send("broadcastRound")

      object challenge:
        val create = send("challengeCreate")
        val accept = send("challengeAccept")
    val googleTokenTime = timer("push.send.googleToken").withoutTags()
    def firebaseStatus(status: Int) = counter("push.firebase.status").withTag("status", status)
    def firebaseType(typ: String) = counter("push.firebase.msgType").withTag("type", typ)
  object fishnet:
    object client:
      object result:
        private val c = counter("fishnet.client.result")
        private def apply(r: String)(client: UserId) =
          c.withTags(tags("client" -> client, "result" -> r))
        val success = apply("success")
        val failure = apply("failure")
        val timeout = apply("timeout")
        val notFound = apply("notFound")
        val notAcquired = apply("notAcquired")
        val abort = apply("abort")
      def status(enabled: Boolean) = gauge("fishnet.client.status").withTag("enabled", enabled)
      def version(v: String) = gauge("fishnet.client.version").withTag("version", v.escape)
    def queueTime(sender: "system" | "user") = timer("fishnet.queue.db").withTag("sender", sender)
    val acquire = future("fishnet.acquire")
    def work(typ: String, as: "system" | "user") =
      gauge("fishnet.work").withTags(tags("type" -> typ, "for" -> as))
    def oldest(as: "system" | "user") = gauge("fishnet.oldest").withTag("for", as)
    object analysis:
      object by:
        def movetime(client: UserId) = histogram("fishnet.analysis.movetime").withTag("client", client)
        def node(client: UserId) = histogram("fishnet.analysis.node").withTag("client", client)
        def nps(client: UserId) = histogram("fishnet.analysis.nps").withTag("client", client)
        def depth(client: UserId) = histogram("fishnet.analysis.depth").withTag("client", client)
        def pvSize(client: UserId) = histogram("fishnet.analysis.pvSize").withTag("client", client)
        def pv(client: UserId, isLong: Boolean) =
          counter("fishnet.analysis.pvs").withTags(tags("client" -> client, "long" -> isLong))
        def totalMeganode(client: UserId) =
          counter("fishnet.analysis.total.meganode").withTag("client", client)
        def totalSecond(client: UserId) =
          counter("fishnet.analysis.total.second").withTag("client", client)
      def requestCount(tpe: "game" | "study") = counter("fishnet.analysis.request").withTag("type", tpe)
      val evalCacheHits = histogram("fishnet.analysis.evalCacheHits").withoutTags()
      val skipPositionsGame = future("fishnet.analysis.skipPositions.game")
      val skipPositionsStudy = future("fishnet.analysis.skipPositions.study")
    object http:
      def request(hit: Boolean) = counter("fishnet.http.acquire").withTag("hit", hit)
    def move(level: Int) = counter("fishnet.move.time").withTag("level", level)
    def openingBook(variant: Variant, hit: Boolean) =
      timer("fishnet.opening.hit").withTags:
        tags("variant" -> variant.key, "hit" -> hitTag(hit))
  object opening:
    def searchTime = timer("opening.search.time").withoutTags()
  object study:
    object tree:
      val read = timer("study.tree.read").withoutTags()
      val write = timer("study.tree.write").withoutTags()
    object sequencer:
      val chapterTime = timer("study.sequencer.chapter.time").withoutTags()
  object api:
    val users = counter("api.cost").withTag("endpoint", "users")
    val activity = counter("api.cost").withTag("endpoint", "activity")
    object challenge:
      object bulk:
        def scheduleNb(by: UserId) = counter("api.challenge.bulk.schedule.nb").withTag("by", by)
        def createNb(by: UserId) = counter("api.challenge.bulk.create.nb").withTag("by", by)
  object `export`:
    object png:
      val game = counter("export.png").withTag("type", "game")
      val puzzle = counter("export.png").withTag("type", "puzzle")
  object bus:
    val classifiers = gauge("bus.classifiers").withoutTags()
  object blocking:
    def time(name: String) = timer("blocking.time").withTag("name", name)
    def timeout(name: String) = counter("blocking.timeout").withTag("name", name)
  object workQueue:
    def offerFail(name: String, result: String) =
      counter("workQueue.offerFail").withTags:
        tags("name" -> name, "result" -> result)
    def timeout(name: String) = counter("workQueue.timeout").withTag("name", name)
  class parallelQueue(name: String):
    val parallelism = gauge("parallelQueue.parallelism").withTag("name", name)
    val computeTimeout = counter("parallelQueue.buildTimeout").withTag("name", name)
  object markdown:
    val time = timer("markdown.time").withoutTags()
  object ublog:
    def create(user: UserId) = counter("ublog.create").withTag("user", user)
    def view = counter("ublog.view").withoutTags()
    object automod:
      val request = future("ublog.automod.request")
      def quality(q: String) = counter("ublog.automod.quality").withTag("quality", q)
      def flagged(f: Boolean) = counter("ublog.automod.flagged").withTag("flagged", f)
  object picfit:
    def uploadTime(user: UserId) = future("picfit.upload.time", tags("user" -> user))
    def uploadSize(user: UserId) = histogram("picfit.upload.size").withTag("user", user)
  object fideSync:
    val time = future("fide.sync.time")
    val players = gauge("fide.sync.players").withoutTags()
    val updated = gauge("fide.sync.updated").withoutTags()
    val deleted = gauge("fide.sync.deleted").withoutTags()
  object link:
    def external(tag: String, auth: Boolean) = counter("link.external").withTags:
      tags("tag" -> tag.escape, "auth" -> auth)
  object recap:
    val games = future("recap.build.games.time")
    val puzzles = future("recap.build.puzzles.time")

  object jvm:
    def threads() =
      val perState = gauge("jvm.threads.group")
      val total = gauge("jvm.threads.group.total")
      for
        group <- scalalib.Jvm.threadGroups()
        _ = total.withTags(tags("name" -> group.name)).update(group.total)
        (state, count) <- group.states
      yield perState.withTags(tags("name" -> group.name, "state" -> state.toString)).update(count)

  object prometheus:
    val lines = gauge("prometheus.lines").withoutTags()

  def chronoSync[A] = lila.common.Chronometer.syncMon[A]

  type TimerPath = lila.mon.type => Timer
  type CounterPath = lila.mon.type => Counter

  private def future(name: String) = (success: Boolean) => timer(name).withTag("success", successTag(success))
  private def future(name: String, tags: Map[String, Any]) = (success: Boolean) =>
    timer(name).withTags(tags + ("success" -> successTag(success)))
  private def future(name: String, segment: String)(success: Boolean) =
    timer(name).withTags:
      tags("success" -> successTag(success), "segment" -> segment)

  private def successTag(success: Boolean) = if success then "success" else "failure"
  private def hitTag(hit: Boolean) = if hit then "hit" else "miss"

  private def apiTag(api: Option[ApiVersion]) = api.fold("-")(_.toString)

  import scala.language.implicitConversions
  private given Conversion[UserId, String] = _.value
  private given Conversion[Map[String, Any], TagSet] = TagSet.from
