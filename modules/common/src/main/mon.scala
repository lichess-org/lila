package lila

import scala.concurrent.Future

import kamon.Kamon.{ metrics, tracer }
import kamon.trace.{ TraceContext, Segment, Status }
import kamon.util.RelativeNanoTimestamp

object mon {

  object http {
    object request {
      val all = inc("http.request.all")
      val ipv6 = inc("http.request.ipv6")
      def path(p: String) = inc(s"http.request.path.$p")
    }
    object response {
      val code400 = inc("http.response.4.00")
      val code403 = inc("http.response.4.03")
      val code404 = inc("http.response.4.04")
      val code500 = inc("http.response.5.00")
      val home = rec("http.response.home")
      object user {
        object show {
          val website = rec("http.response.user.show.website")
          val mobile = rec("http.response.user.show.mobile")
        }
      }
      object tournament {
        object show {
          val website = rec("http.response.tournament.show.website")
          val mobile = rec("http.response.tournament.show.mobile")
        }
      }
      object player {
        val website = rec("http.response.player.website")
        val mobile = rec("http.response.player.mobile")
      }
      object watcher {
        val website = rec("http.response.watcher.website")
        val mobile = rec("http.response.watcher.mobile")
      }
    }
    object prismic {
      val timeout = inc("http.prismic.timeout")
    }
    object mailgun {
      val timeout = inc("http.mailgun.timeout")
    }
    object userGames {
      def cost = incX("http.user-games.cost")
    }
    object csrf {
      val missingOrigin = inc("http.csrf.missing_origin")
      val forbidden = inc("http.csrf.forbidden")
      val websocket = inc("http.csrf.websocket")
    }
  }
  object mobile {
    def version(v: String) = inc(s"mobile.version.$v")
  }
  object syncache {
    def miss(name: String) = inc(s"syncache.miss.$name")
    def wait(name: String) = inc(s"syncache.wait.$name")
    def preload(name: String) = inc(s"syncache.preload.$name")
    def timeout(name: String) = inc(s"syncache.timeout.$name")
    def waitMicros(name: String) = incX(s"syncache.wait_micros.$name")
    def computeNanos(name: String) = rec(s"syncache.compute_nanos.$name")
  }
  class Caffeine(name: String) {
    val hitCount = rec(s"caffeine.count.hit.$name")
    val hitRate = rate(s"caffeine.rate.hit.$name")
    val missCount = rec(s"caffeine.count.miss.$name")
    val loadSuccessCount = rec(s"caffeine.count.load.success.$name")
    val loadFailureCount = rec(s"caffeine.count.load.failure.$name")
    val totalLoadTime = rec(s"caffeine.total.load_time.$name") // in millis
    val averageLoadPenalty = rec(s"caffeine.penalty.load_time.$name")
    val evictionCount = rec(s"caffeine.count.eviction.$name")
    val entryCount = rec(s"caffeine.count.entry.$name")
  }
  object evalCache {
    private val hit = inc("eval_Cache.all.hit")
    private val miss = inc("eval_Cache.all.miss")
    private def hitIf(cond: Boolean) = if (cond) hit else miss
    private object byPly {
      def hit(ply: Int) = inc(s"eval_Cache.ply.$ply.hit")
      def miss(ply: Int) = inc(s"eval_Cache.ply.$ply.miss")
      def hitIf(ply: Int, cond: Boolean) = if (cond) hit(ply) else miss(ply)
    }
    def register(ply: Int, isHit: Boolean) = {
      hitIf(isHit)()
      if (ply <= 10) byPly.hitIf(ply, isHit)()
    }
  }
  object lobby {
    object hook {
      val create = inc("lobby.hook.create")
      val join = inc("lobby.hook.join")
      val size = rec("lobby.hook.size")
      def joinMobile(isMobile: Boolean) = inc(s"lobby.hook.join_mobile.$isMobile")
      def createdLikePoolFiveO(isMobile: Boolean) = inc(s"lobby.hook.like_pool_5_0.$isMobile")
      def acceptedLikePoolFiveO(isMobile: Boolean) = inc(s"lobby.hook.like_pool_5_0_accepted.$isMobile")
    }
    object seek {
      val create = inc("lobby.seek.create")
      val join = inc("lobby.seek.join")
      def joinMobile(isMobile: Boolean) = inc(s"lobby.seek.join_mobile.$isMobile")
    }
    object socket {
      val getUids = rec("lobby.socket.get_uids")
      val member = rec("lobby.socket.member")
      val idle = rec("lobby.socket.idle")
      val hookSubscribers = rec("lobby.socket.hook_subscribers")
      val mobile = rec(s"lobby.socket.mobile")
    }
    object pool {
      object wave {
        def scheduled(id: String) = inc(s"lobby.pool.$id.wave.scheduled")
        def full(id: String) = inc(s"lobby.pool.$id.wave.full")
        def candidates(id: String) = rec(s"lobby.pool.$id.wave.candidates")
        def paired(id: String) = rec(s"lobby.pool.$id.wave.paired")
        def missed(id: String) = rec(s"lobby.pool.$id.wave.missed")
        def wait(id: String) = rec(s"lobby.pool.$id.wave.wait")
        def ratingDiff(id: String) = rec(s"lobby.pool.$id.wave.rating_diff")
        def withRange(id: String) = rec(s"lobby.pool.$id.wave.with_range")
      }
      object thieve {
        def timeout(id: String) = inc(s"lobby.pool.$id.thieve.timeout")
        def candidates(id: String) = rec(s"lobby.pool.$id.thieve.candidates")
        def stolen(id: String) = rec(s"lobby.pool.$id.thieve.stolen")
      }
      object join {
        def count(id: String) = inc(s"lobby.pool.$id.join.count")
      }
      object leave {
        def count(id: String) = inc(s"lobby.pool.$id.leave.count")
        def wait(id: String) = rec(s"lobby.pool.$id.leave.wait")
      }
      object matchMaking {
        def duration(id: String) = rec(s"lobby.pool.$id.match_making.duration")
      }
      object gameStart {
        def duration(id: String) = rec(s"lobby.pool.$id.game_start.duration")
      }
    }
  }
  object round {
    object api {
      val player = rec("round.api.player")
      val playerInner = rec("round.api.player_inner")
      val watcher = rec("round.api.watcher")
    }
    object actor {
      val count = rec("round.actor.count")
    }
    object forecast {
      val create = inc("round.forecast.create")
    }
    object move {
      object full {
        val count = inc("round.move.full")
      }
      object trace {
        def create = makeTrace("round.move.trace")
      }
      object lag {
        val compDeviation = rec("round.move.lag.comp_deviation")
        def uncomped(key: String) = rec(s"round.move.lag.uncomped_ms.$key")
        val uncompedAll = rec(s"round.move.lag.uncomped_ms.all")
        def uncompStdDev(key: String) = rec(s"round.move.lag.uncomp_stdev_ms.$key")
        val stdDev = rec(s"round.move.lag.stddev_ms")
        val mean = rec(s"round.move.lag.mean_ms")
        val coefVar = rec(s"round.move.lag.coef_var_1000")
        val compEstStdErr = rec(s"round.move.lag.comp_est_stderr_1000")
        val compEstOverErr = rec("round.move.lag.avg_over_error_ms")
      }
    }
    object error {
      val client = inc("round.error.client")
      val fishnet = inc("round.error.fishnet")
      val glicko = inc("round.error.glicko")
    }
    object titivate {
      val time = rec("round.titivate.time")
      val game = rec("round.titivate.game") // how many games were processed
      val total = rec("round.titivate.total") // how many games should have been processed
      val old = rec("round.titivate.old") // how many old games remain
    }
    object alarm {
      val time = rec("round.alarm.time")
      val count = rec("round.alarm.count")
    }
    object expiration {
      val count = inc("round.expiration.count")
    }
    def proxyGameWatcherCount(result: String) = inc(s"round.proxy_game.watcher.$result")
    val proxyGameWatcherTime = rec("round.proxy_game.watcher.time")
  }
  object playban {
    def outcome(out: String) = inc(s"playban.outcome.$out")
    object ban {
      val count = inc("playban.ban.count")
      val mins = incX("playban.ban.mins")
    }
  }
  object explorer {
    object index {
      val success = incX("explorer.index.success")
      val failure = incX("explorer.index.failure")
      val time = rec("explorer.index.time")
    }
  }
  object timeline {
    val notification = incX("timeline.notification")
  }
  object insight {
    object request {
      val count = inc("insight.request")
      val time = rec("insight.request")
    }
    object index {
      val count = inc("insight.index")
      val time = rec("insight.index")
    }
  }
  object search {
    def client(op: String) = rec(s"search.client.$op")
    def success(op: String) = inc(s"search.client.$op.success")
    def failure(op: String) = inc(s"search.client.$op.failure")
  }
  object study {
    object search {
      object index {
        def count = inc("study.search.index.count")
        def time = rec("study.search.index.time")
      }
      object query {
        def count = inc("study.search.query.count")
        def time = rec("study.search.query.time")
      }
    }
  }
  object jvm {
    val thread = rec("jvm.thread")
    val daemon = rec("jvm.daemon")
    val uptime = rec("jvm.uptime")
  }
  object user {
    val online = rec("user.online")
    object register {
      val website = inc("user.register.website")
      val mobile = inc("user.register.mobile")
      def mustConfirmEmail(v: String) = inc(s"user.register.must_confirm_email.$v")
      def confirmEmailResult(v: Boolean) = inc(s"user.register.confirm_email.$v")
      val modConfirmEmail = inc(s"user.register.mod_confirm_email")
    }
    object auth {
      val bcFullMigrate = inc("user.auth.bc_full_migrate")
      val hashTime = rec("user.auth.hash_time")
      val hashTimeInc = incX("user.auth.hash_time_inc")
      def result(v: Boolean) = inc(s"user.auth.result.$v")

      def passwordResetRequest(s: String) = inc(s"user.auth.password_reset_request.$s")
      def passwordResetConfirm(s: String) = inc(s"user.auth.password_reset_confirm.$s")
    }
    object oauth {
      object usage {
        val success = inc("user.oauth.usage.success")
        val failure = inc("user.oauth.usage.success")
      }
    }
  }
  object socket {
    val member = rec("socket.count")
    val open = inc("socket.open")
    val close = inc("socket.close")
    def eject(userId: String) = inc(s"socket.eject.user.$userId")
    val ejectAll = inc(s"socket.eject.all")
  }
  object mod {
    object report {
      val unprocessed = rec("mod.report.unprocessed")
      val close = inc("mod.report.close")
      def create(reason: String) = inc(s"mod.report.create.$reason")
      def discard(reason: String) = inc(s"mod.report.discard.$reason")
    }
    object log {
      val create = inc("mod.log.create")
    }
    object irwin {
      val report = inc("mod.report.irwin.report")
      val mark = inc("mod.report.irwin.mark")
      def ownerReport(name: String) = inc(s"mod.irwin.owner_report.$name")
      def streamEventType(name: String) = inc(s"mod.irwin.streama.event_type.$name") // yes there's a typo
      object assessment {
        val count = inc("mod.irwin.assessment.count")
        val time = rec("mod.irwin.assessment.time")
      }
    }
  }
  object relay {
    val ongoing = rec("relay.ongoing")
    val moves = incX("relay.moves")
    object sync {
      def result(res: String) = inc(s"relay.sync.result.$res")
      object duration {
        val each = rec("relay.sync.duration.each")
      }
    }
  }
  object cheat {
    val cssBot = inc("cheat.css_bot")
    val holdAlert = inc("cheat.hold_alert")
    object autoAnalysis {
      def reason(r: String) = inc(s"cheat.auto_analysis.reason.$r")
    }
    object autoMark {
      val count = inc("cheat.auto_mark.count")
    }
    object autoReport {
      val count = inc("cheat.auto_report.count")
    }
  }
  object email {
    val resetPassword = inc("email.reset_password")
    val fix = inc("email.fix")
    val change = inc("email.change")
    val confirmation = inc("email.confirmation")
    val disposableDomain = rec("email.disposable_domain")
  }
  object security {
    object tor {
      val node = rec("security.tor.node")
    }
    object firewall {
      val block = inc("security.firewall.block")
      val ip = rec("security.firewall.ip")
    }
    object proxy {
      object request {
        val success = inc("security.proxy.success")
        val failure = inc("security.proxy.failure")
        val time = rec("security.proxy.request")
      }
      val percent = rec("security.proxy.percent")
    }
    object rateLimit {
      def generic(key: String) = inc(s"security.rate_limit.generic.$key")
    }
    object linearLimit {
      def generic(key: String) = inc(s"security.linear_limit.generic.$key")
    }
  }
  object tv {
    object stream {
      val count = rec("tv.streamer.count")
      def name(n: String) = rec(s"tv.streamer.name.$n")
    }
  }
  object relation {
    val follow = inc("relation.follow")
    val unfollow = inc("relation.unfollow")
    val block = inc("relation.block")
    val unblock = inc("relation.unblock")
  }
  object coach {
    object pageView {
      def profile(coachId: String) = inc(s"coach.page_view.profile.$coachId")
    }
  }
  object tournament {
    object pairing {
      val create = incX("tournament.pairing.create")
      val createTime = rec("tournament.pairing.create_time")
      val prepTime = rec("tournament.pairing.prep_time")
      val cutoff = inc("tournament.pairing.cutoff")
      val giveup = inc("tournament.pairing.giveup")
    }
    val created = rec("tournament.created")
    val started = rec("tournament.started")
    val player = rec("tournament.player")
    object startedOrganizer {
      val tickTime = rec("tournament.started_organizer.tick_time")
    }
    object createdOrganizer {
      val tickTime = rec("tournament.created_organizer.tick_time")
    }
  }
  object plan {
    object amount {
      val paypal = incX("plan.amount.paypal")
      val stripe = incX("plan.amount.stripe")
    }
    object count {
      val paypal = inc("plan.count.paypal")
      val stripe = inc("plan.count.stripe")
    }
    val goal = rec("plan.goal")
    val current = rec("plan.current")
    val percent = rec("plan.percent")
  }
  object forum {
    object post {
      val create = inc("forum.post.create")
    }
    object topic {
      val view = inc("forum.topic.view")
    }
  }
  object puzzle {
    object selector {
      val count = inc("puzzle.selector")
      val time = rec("puzzle.selector")
      def vote(v: Int) = rec("puzzle.selector.vote")(1000 + v) // vote sum of selected puzzle
    }
    object batch {
      object selector {
        val count = incX("puzzle.batch.selector")
        val time = rec("puzzle.batch.selector")
      }
      val solve = incX("puzzle.batch.solve")
    }
    object round {
      val user = inc("puzzle.attempt.user")
      val anon = inc("puzzle.attempt.anon")
      val mate = inc("puzzle.attempt.mate")
      val material = inc("puzzle.attempt.material")
    }
    object vote {
      val up = inc("puzzle.vote.up")
      val down = inc("puzzle.vote.down")
    }
    val crazyGlicko = inc("puzzle.crazy_glicko")
  }
  object opening {
    object selector {
      val count = inc("opening.selector")
      val time = rec("opening.selector")
    }
    val crazyGlicko = inc("opening.crazy_glicko")
  }
  object game {
    def finish(status: String) = inc(s"game.finish.$status")
    object create {
      def variant(v: String) = inc(s"game.create.variant.$v")
      def speed(v: String) = inc(s"game.create.speed.$v")
      def source(v: String) = inc(s"game.create.source.$v")
      def mode(v: String) = inc(s"game.create.mode.$v")
    }
    val fetch = inc("game.fetch.count")
    val fetchLight = inc("game.fetchLight.count")
    val loadClockHistory = inc("game.loadClockHistory.count")
    object pgn {
      final class Protocol(name: String) {
        val count = inc(s"game.pgn.$name.count")
        val time = rec(s"game.pgn.$name.time")
      }
      object oldBin {
        val encode = new Protocol("oldBin.encode")
        val decode = new Protocol("oldBin.decode")
      }
      object huffman {
        val encode = new Protocol("huffman.encode")
        val decode = new Protocol("huffman.decode")
      }
    }
  }
  object chat {
    val message = inc("chat.message")
  }
  object push {
    object register {
      def in(platform: String) = inc(s"push.register.in.$platform")
      def out = inc(s"push.register.out")
    }
    object send {
      def move(platform: String) = inc(s"push.send.$platform.move")()
      def takeback(platform: String) = inc(s"push.send.$platform.takeback")()
      def corresAlarm(platform: String) = inc(s"push.send.$platform.corresAlarm")()
      def finish(platform: String) = inc(s"push.send.$platform.finish")()
      def message(platform: String) = inc(s"push.send.$platform.message")()
      object challenge {
        def create(platform: String) = inc(s"push.send.$platform.challenge_create")()
        def accept(platform: String) = inc(s"push.send.$platform.challenge_accept")()
      }
    }
  }
  object fishnet {
    object client {
      def result(client: String, skill: String) = new {
        def success = apply("success")
        def failure = apply("failure")
        def weak = apply("weak")
        def timeout = apply("timeout")
        def notFound = apply("not_found")
        def notAcquired = apply("not_acquired")
        def abort = apply("abort")
        private def apply(r: String) = inc(s"fishnet.client.result.$skill.$client.$r")
      }
      object status {
        val enabled = rec("fishnet.client.status.enabled")
        val disabled = rec("fishnet.client.status.disabled")
      }
      def skill(v: String) = rec(s"fishnet.client.skill.$v")
      def version(v: String) = rec(s"fishnet.client.version.${makeVersion(v)}")
      def stockfish(v: String) = rec(s"fishnet.client.engine.stockfish.${makeVersion(v)}")
      def python(v: String) = rec(s"fishnet.client.python.${makeVersion(v)}")
    }
    object queue {
      def db(skill: String) = rec(s"fishnet.queue.db.$skill")
      def sequencer(skill: String) = rec(s"fishnet.queue.sequencer.$skill")
    }
    object acquire {
      def time(skill: String) = rec(s"fishnet.acquire.skill.$skill")
      def timeout(skill: String) = inc(s"fishnet.acquire.timeout.skill.$skill")
    }
    object work {
      def acquired(skill: String) = rec(s"fishnet.work.$skill.acquired")
      def queued(skill: String) = rec(s"fishnet.work.$skill.queued")
      def forUser(skill: String) = rec(s"fishnet.work.$skill.for_user")
      val moveDbSize = rec("fishnet.work.move.db_size")
    }
    object move {
      def time(client: String) = rec(s"fishnet.move.time.$client")
      def fullTimeLvl1(client: String) = rec(s"fishnet.move.full_time_lvl_1.$client")
      val post = rec("fishnet.move.post")
      val dbDrop = inc("fishnet.move.db_drop")
    }
    object analysis {
      def by(client: String) = new {
        def hash = rec(s"fishnet.analysis.hash.$client")
        def threads = rec(s"fishnet.analysis.threads.$client")
        def movetime = rec(s"fishnet.analysis.movetime.$client")
        def node = rec(s"fishnet.analysis.node.$client")
        def nps = rec(s"fishnet.analysis.nps.$client")
        def depth = rec(s"fishnet.analysis.depth.$client")
        def pvSize = rec(s"fishnet.analysis.pv_size.$client")
        def pvTotal = incX(s"fishnet.analysis.pvs.total.$client")
        def pvShort = incX(s"fishnet.analysis.pvs.short.$client")
        def pvLong = incX(s"fishnet.analysis.pvs.long.$client")
        def totalMeganode = incX(s"fishnet.analysis.total.meganode.$client")
        def totalSecond = incX(s"fishnet.analysis.total.second.$client")
        def totalPosition = incX(s"fishnet.analysis.total.position.$client")
      }
      val post = rec("fishnet.analysis.post")
      val requestCount = inc("fishnet.analysis.request")
      val evalCacheHits = rec("fishnet.analysis.eval_cache_hits")
    }
  }
  object api {
    object teamUsers {
      val cost = incX("api.team-users.cost")
    }
    object userGames {
      val cost = incX("api.user-games.cost")
    }
    object users {
      val cost = incX("api.users.cost")
    }
    object game {
      val cost = incX("api.game.cost")
    }
    object activity {
      val cost = incX("api.activity.cost")
    }
  }
  object export {
    object pgn {
      def game = inc("export.pgn.game")
      def study = inc("export.pgn.study")
      def studyChapter = inc("export.pgn.study_chapter")
    }
    object png {
      def game = inc("export.png.game")
      def puzzle = inc("export.png.puzzle")
    }
    def pdf = inc("export.pdf.game")
  }

  object jsmon {
    val socketGap = inc("jsmon.socket_gap")
    val unknown = inc("jsmon.unknown")
  }

  def measure[A](path: RecPath)(op: => A): A = measureRec(path(this))(op)
  def measureRec[A](rec: Rec)(op: => A): A = {
    val start = System.nanoTime()
    val res = op
    rec(System.nanoTime() - start)
    res
  }
  def measureIncMicros[A](path: IncXPath)(op: => A): A = {
    val start = System.nanoTime()
    val res = op
    path(this)(((System.nanoTime() - start) / 1000).toInt)
    res
  }

  def since[A](path: RecPath)(start: Long) = path(this)(System.nanoTime() - start)

  type Rec = Long => Unit
  type Inc = () => Unit
  type IncX = Int => Unit
  type Rate = Double => Unit

  type RecPath = lila.mon.type => Rec
  type IncPath = lila.mon.type => Inc
  type IncXPath = lila.mon.type => IncX

  def recPath(f: lila.mon.type => Rec): Rec = f(this)
  def incPath(f: lila.mon.type => Inc): Inc = f(this)

  private def inc(name: String): Inc = metrics.counter(name).increment _
  private def incX(name: String): IncX = {
    val count = metrics.counter(name)
    value => {
      if (value < 0) logger.warn(s"Negative increment value: $name=$value")
      else count.increment(value)
    }
  }
  private def rec(name: String): Rec = {
    val hist = metrics.histogram(name)
    value => {
      if (value < 0) logger.warn(s"Negative histogram value: $name=$value")
      else hist.record(value)
    }
  }

  // to record Double rates [0..1],
  // we multiply by 100,000 and convert to Int [0..100000]
  private def rate(name: String): Rate = {
    val hist = metrics.histogram(name)
    value => {
      if (value < 0) logger.warn(s"Negative histogram value: $name=$value")
      else hist.record((value * 100000).toInt)
    }
  }

  final class Measurement(since: Long, path: RecPath) {
    def finish() = path(lila.mon)(System.nanoTime() - since)
  }

  def startMeasurement(path: RecPath) = new Measurement(System.nanoTime(), path)

  trait Trace {

    def finishFirstSegment(): Unit

    def segment[A](name: String, categ: String)(f: => Future[A]): Future[A]

    def segmentSync[A](name: String, categ: String)(f: => A): A

    def finish(): Unit
  }

  private final class KamonTrace(
      context: TraceContext,
      firstSegment: Segment
  ) extends Trace {

    def finishFirstSegment() = firstSegment.finish()

    def segment[A](name: String, categ: String)(code: => Future[A]): Future[A] =
      context.withNewAsyncSegment(name, categ, "mon")(code)

    def segmentSync[A](name: String, categ: String)(code: => A): A =
      context.withNewSegment(name, categ, "mon")(code)

    def finish() = context.finish()
  }

  private def makeTrace(name: String, firstName: String = "first"): Trace = {
    val context = tracer.newContext(
      name = name,
      token = None,
      tags = Map.empty,
      timestamp = RelativeNanoTimestamp.now,
      status = Status.Open,
      isLocal = false
    )
    val firstSegment = context.startSegment(firstName, "logic", "mon")
    new KamonTrace(context, firstSegment)
  }

  private val stripVersionRegex = """[^\w\.\-]""".r
  private def stripVersion(v: String) = stripVersionRegex.replaceAllIn(v, "")
  private def nodots(s: String) = s.replace(".", "_")
  private val makeVersion = nodots _ compose stripVersion _

  private val logger = lila.log("monitor")
}
