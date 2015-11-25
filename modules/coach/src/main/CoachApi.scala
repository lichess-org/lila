package lila.coach

private final class CoachApi(storage: Storage) {

  def ask[X, Dim <: Dimension[X]](question: Question[X, Dim]): Fu[Answer[X, Dim]] = ???
}
