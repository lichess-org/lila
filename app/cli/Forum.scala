package lila
package cli

import lila.forum.ForumEnv
import scalaz.effects._

private[cli] case class Forum(env: ForumEnv) {

  def denormalize: IO[String] = env.denormalize inject "Forum denormalized"

  def typecheck: IO[String] = for {
    _ ← env.categRepo.all
    _ ← env.topicRepo.all
    _ ← env.postRepo.all
  } yield "Forum type checked"
}
