package lila

import lobby.{ Fisherman, History, HookRepo, Hook, MessageRepo }
import timeline.EntryRepo
import game.GameRepo

import scalaz.effects._

final class Preload(
    fisherman: Fisherman,
    history: History,
    hookRepo: HookRepo,
    gameRepo: GameRepo,
    messageRepo: MessageRepo,
    entryRepo: EntryRepo) {

  type Response = Either[String, Map[String, Any]]

  def apply(
    auth: Boolean,
    chat: Boolean,
    myHook: Option[Hook]): IO[Response] = for {
    hooks ← auth.fold(hookRepo.allOpen, hookRepo.allOpenCasual)
    std = () ⇒ stdResponse(chat, hooks, myHook)
    res ← myHook.fold(
      h ⇒ hookResponse(h, std),
      std())
  } yield res

  private def hookResponse(
    myHook: Hook,
    std: () ⇒ IO[Response]): IO[Response] =
    myHook.gameId.fold(
      ref ⇒ gameRepo game ref map { game ⇒
        game.fold(
          g ⇒ redirect(g fullIdOf g.creatorColor),
          redirect()
        )
      },
      fisherman shake myHook flatMap { _ ⇒ std() }
    )

  private def stdResponse(
    chat: Boolean,
    hooks: List[Hook],
    myHook: Option[Hook]): IO[Response] = for {
    messages ← if (chat) messageRepo.recent else io(Nil)
    entries ← entryRepo.recent
  } yield Right(Map(
    "version" -> history.version,
    "pool" -> renderHooks(hooks, myHook).pp,
    "chat" -> (messages.reverse map (_.render)),
    "timeline" -> (entries.reverse map (_.render))
  ))

  private def renderHooks(
    hooks: List[Hook],
    myHook: Option[Hook]) = hooks map { h ⇒
    myHook.exists(_.id == h.id).fold(
      h.render ++ Map("ownerId" -> h.ownerId),
      h.render)
  }

  private def redirect(url: String = "") = Left("/" + url)
}
