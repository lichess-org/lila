package lila

import kamon.Kamon.metrics

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
        val time = rec("round.move.full")
        val count = inc("round.move.full")
      }
      object segment {
        val queue = rec("round.move.segment.queue")
        val fetch = rec("round.move.segment.fetch")
        val logic = rec("round.move.segment.logic")
        val save = rec("round.move.segment.save")
      }
      val networkLag = rec("round.move.network_lag")
    }
    val crazyGlicko = inc("round.crazy_glicko")
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
    }
    object log {
      val create = inc("mod.log.create")
    }
  }
  object cheat {
    val cssBot = inc("cheat.css_bot")
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
      val move = inc("push.send.move")
      val finish = inc("push.send.finish")
      object challenge {
        val create = inc("push.send.challenge.create")
        val accept = inc("push.send.challenge.accept")
      }
    }
  }
  object ai {
    object play {
      def success(source: String) = inc(s"ai.play.${ip(source)}.success")
      def fail(source: String) = inc(s"ai.play.${ip(source)}.fail")
      def time(source: String) = rec(s"ai.play.${ip(source)}")
    }
    object analysis {
      def success(source: String) = inc(s"ai.analysis.${ip(source)}.success")
      def fail(source: String) = inc(s"ai.analysis.${ip(source)}.fail")
      def time(source: String) = rec(s"ai.analysis.${ip(source)}")
    }
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
  private def incX(name: String): IncX = metrics.counter(name).increment(_)
  private def rec(name: String): Rec = metrics.histogram(name).record(_)

  private def ip(ip: String) = ip.replace(".", "_")
}
