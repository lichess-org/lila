package lila
package cli

import lila.forum.ForumEnv
import scalaz.effects._

private[cli] case class Forum(env: ForumEnv) {

  def denormalize: IO[Unit] = env.denormalize

  def typecheck: IO[Unit] = for {
    _ ← env.categRepo.all
    _ ← env.topicRepo.all
    _ ← env.postRepo.all
  } yield ()
}
