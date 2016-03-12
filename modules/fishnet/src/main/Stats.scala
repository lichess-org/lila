package lila.fishnet

case class Stats(
    move: Stats.Result,
    analysis: Stats.Result) {

  def add(work: Work, update: Stats.ResultUpdate) = work match {
    case _: Work.Move     => copy(move = update(move))
    case _: Work.Analysis => copy(analysis = update(analysis))
  }
}

object Stats {

  type ResultUpdate = Stats.Result => Stats.Result

  def emptyResult = Result(0, 0, 0)

  def empty = Stats(emptyResult, emptyResult)

  case class Result(
      acquire: Int,
      success: Int,
      timeout: Int) {

    def addAcquire = copy(acquire = acquire + 1)
    def addSuccess = copy(success = success + 1)
    def addTimeout = copy(timeout = timeout + 1)
  }
}
