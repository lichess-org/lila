package lila.cli

import lila.forum.ForumEnv
import scalaz.effects._

case class Forum(env: ForumEnv) {

  def denormalize: IO[Unit] = env.denormalize

  def typecheck: IO[Unit] = for {
    _ ← env.categRepo.all
    _ ← env.topicRepo.all
    _ ← env.postRepo.all
  } yield ()
}
