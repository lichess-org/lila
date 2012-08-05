package lila.cli

import scalaz.effects._
import play.api.{ Mode, Application, Play }
import java.io.File

import lila.parseIntOption
import lila.core.{ Global, CoreEnv }

object Main {

  def main(args: Array[String]): Unit = withApp { env ⇒

    def users = Users(env.user.userRepo, env.security.store)
    def games = Games(env)
    def i18n = I18n(env.i18n)
    def titivate = env.titivate
    def forum = Forum(env.forum)
    def infos = Infos(env)
    def wiki = Wiki(env.wiki)

    args.toList match {
      case "average-elo" :: Nil               ⇒ infos.averageElo
      case "i18n-js-dump" :: Nil              ⇒ i18n.jsDump
      case "i18n-fix" :: Nil                  ⇒ i18n.fileFix
      case "i18n-fetch" :: from :: Nil        ⇒ i18n fetch from
      case "user-enable" :: uid :: Nil        ⇒ users enable uid
      case "user-disable" :: uid :: Nil       ⇒ users disable uid
      case "user-passwd" :: uid :: pwd :: Nil ⇒ users.passwd(uid, pwd)
      case "user-track" :: uid :: Nil         ⇒ users track uid
      case "forum-denormalize" :: Nil         ⇒ forum.denormalize
      case "forum-typecheck" :: Nil           ⇒ forum.typecheck
      case "game-cleanup-next" :: Nil         ⇒ titivate.cleanupNext
      case "game-cleanup-unplayed" :: Nil     ⇒ titivate.cleanupUnplayed
      case "game-finish" :: Nil               ⇒ titivate.finishByClock
      case "game-per-day" :: Nil              ⇒ games.perDay(30)
      case "game-per-day" :: days :: Nil      ⇒ games.perDay(parseIntOption(days) err "days: Int")
      case "wiki-fetch" :: Nil                ⇒ wiki.fetch
      case _ ⇒
        putStrLn("Unknown command: " + args.mkString(" "))
    }
  }

  private def withApp(op: CoreEnv ⇒ IO[_]): Int = {
    val app = new Application(
      path = new File("."),
      classloader = this.getClass.getClassLoader,
      sources = None,
      mode = Mode.Test)
    try {
      Play start app
      op(Global.env).unsafePerformIO
      0
    }
    finally {
      Play.stop()
    }
  }
}
