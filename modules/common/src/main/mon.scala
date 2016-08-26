package lila

import scala.concurrent.Future

import kamon.Kamon.{ metrics, tracer }
import kamon.trace.{ TraceContext, Segment }
import kamon.util.RelativeNanoTimestamp

object mon {

  object http {
    object request {
      val all = inc("http.request.all")
    }
    object response {
      val code400 = inc("http.response.4.00")
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
  }
  object lobby {
    object hook {
      val create = inc("lobby.hook.create")
      val join = inc("lobby.hook.join")
      val size = rec("lobby.hook.size")
    }
    object seek {
      val create = inc("lobby.seek.create")
      val join = inc("lobby.seek.join")
    }
    object socket {
      val getUids = rec("lobby.socket.get_uids")
      val member = rec("lobby.socket.member")
      val resync = inc("lobby.socket.resync")
    }
  }
  object round {
    object api {
      val player = rec("round.api.player")
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
      val networkLag = rec("round.move.network_lag")
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
    }
  }
  object socket {
    val member = rec("socket.count")
    val open = inc("socket.open")
    val close = inc("socket.close")
  }
  object mod {
    object report {
      val unprocessed = rec("mod.report.unprocessed")
      val close = inc("mod.report.close")
      def create(reason: String) = inc(s"mod.report.create.$reason")
    }
    object log {
      val create = inc("mod.log.create")
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
  object tournament {
    object pairing {
      val create = incX("tournament.pairing.create")
      val createTime = rec("tournament.pairing.create_time")
      val cutoff = inc("tournament.pairing.cutoff")
      val giveup = inc("tournament.pairing.giveup")
    }
    val created = rec("tournament.created")
    val started = rec("tournament.started")
    val player = rec("tournament.player")
  }
  object donation {
    val goal = rec("donation.goal")
    val current = rec("donation.current")
    val percent = rec("donation.percent")
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
      def finish(platform: String) = inc(s"push.send.$platform.finish")()
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
      def sunsetter(v: String) = rec(s"fishnet.client.engine.sunsetter.${makeVersion(v)}")
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
      def moveDbSize = rec(s"fishnet.work.move.db_size")
    }
    object move {
      def time(client: String) = rec(s"fishnet.move.time.$client")
      def post = rec(s"fishnet.move.post")
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
        def totalMeganode = incX(s"fishnet.analysis.total.meganode.$client")
        def totalSecond = incX(s"fishnet.analysis.total.second.$client")
        def totalPosition = incX(s"fishnet.analysis.total.position.$client")
      }
      def post = rec(s"fishnet.analysis.post")
    }
  }
  object api {
    object teamUsers {
      def cost = incX(s"api.team-users.cost")
    }
    object userGames {
      def cost = incX(s"api.user-games.cost")
    }
    object game {
      def cost = incX(s"api.game.cost")
    }
  }
  object export {
    object pgn {
      def game = inc("export.pgn.game")
      def study = inc("export.pgn.study")
    }
    object png {
      def game = inc("export.png.game")
      def puzzle = inc("export.png.puzzle")
    }
    def pdf = inc("export.pdf.game")
  }

  def measure[A](path: RecPath)(op: => A) = {
    val start = System.nanoTime()
    val res = op
    path(this)(System.nanoTime() - start)
    res
  }

  def since[A](path: RecPath)(start: Long) = path(this)(System.nanoTime() - start)

  type Rec = Long => Unit
  type Inc = () => Unit
  type IncX = Int => Unit

  type RecPath = lila.mon.type => Rec
  type IncPath = lila.mon.type => Inc

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
      firstSegment: Segment) extends Trace {

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
      timestamp = RelativeNanoTimestamp.now,
      isOpen = true,
      isLocal = false)
    val firstSegment = context.startSegment(firstName, "logic", "mon")
    new KamonTrace(context, firstSegment)
  }

  private val stripVersionRegex = """[^\w\.\-]""".r
  private def stripVersion(v: String) = stripVersionRegex.replaceAllIn(v, "")
  private def nodots(s: String) = s.replace(".", "_")
  private val makeVersion = nodots _ compose stripVersion _

  private val logger = lila.log("monitor")
}
