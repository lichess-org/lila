package lila.challenge

import org.joda.time.DateTime

private final class Sweeper(
    api: ChallengeApi,
    repo: ChallengeRepo) {

  def realTime: Funit = repo.unseenSince(DateTime.now minusSeconds 10, max = 50) flatMap { cs =>
    lila.common.Future.applySequentially(cs)(api.abandon).void
  }
}
