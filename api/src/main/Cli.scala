package lila.api

import play.api.libs.concurrent.Execution.Implicits._

private[api] final class Cli(env: Env) {

  private def wiki = lila.wiki.Env.current.cli
  private def user = lila.user.Env.current.cli
  private def security = lila.security.Env.current.cli

  def apply(args: List[String]): Fu[String] = ({
    lila.forum.Env.current.cli.process orElse 
    lila.security.Env.current.cli.process 
  } lift args) | fufail("Unknown command: " + args.mkString(" "))

  def apply2(args: List[String]): Fu[String] = (((args match {
    // case "average-elo" :: Nil               ⇒ infos.averageElo
    // case "i18n-js-dump" :: Nil              ⇒ i18n.jsDump
    // case "i18n-fix" :: Nil                  ⇒ i18n.fileFix
    // case "i18n-fetch" :: from :: Nil        ⇒ i18n fetch from
    // case "user-rewrite-history" :: Nil      ⇒ users.rewriteHistory
    // case "forum-denormalize" :: Nil         ⇒ forum.denormalize
    // case "forum-typecheck" :: Nil           ⇒ forum.typecheck
    // case "forum-search" :: text :: Nil      ⇒ forum.search(text)
    // case "forum-search-reset" :: Nil        ⇒ forum.searchReset
    // case "game-cleanup-next" :: Nil         ⇒ titivate.cleanupNext inject "Done"
    // case "game-cleanup-unplayed" :: Nil     ⇒ titivate.cleanupUnplayed inject "Done"
    // case "game-finish" :: Nil               ⇒ titivate.finishByClock inject "Done"
    // case "game-per-day" :: Nil              ⇒ games.perDay(30)
    // case "game-per-day" :: days :: Nil      ⇒ games.perDay(parseIntOption(days) err "days: Int")
    case "wiki-fetch" :: Nil                    ⇒ wiki.fetch
    // case "search-reset" :: Nil              ⇒ search.reset
    // case "team-search" :: text :: Nil       ⇒ teams.search(text)
    // case "team-search-reset" :: Nil         ⇒ teams.searchReset
    // case "team-join" :: team :: users       ⇒ teams.join(team, users)
    // case "team-quit" :: team :: users       ⇒ teams.quit(team, users)
    // case "team-enable" :: uid :: Nil        ⇒ teams enable uid
    // case "team-disable" :: uid :: Nil       ⇒ teams disable uid
    // case "team-delete" :: uid :: Nil        ⇒ teams delete uid
    case _                                      ⇒ fufail("Unknown command: " + args.mkString(" "))
  }) recover {
    case throwable ⇒ s"ERROR $throwable"
    }) map (_ + "\n")) ~ {
    _ onSuccess {
      case output ⇒ println("[cli] %s\n%s".format(args mkString " ", output))
    }
  }

}
