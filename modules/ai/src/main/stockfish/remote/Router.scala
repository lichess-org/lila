package lila.ai
package stockfish
package remote

private[ai] final class Router(
    url: String,
    playRoute: String,
    analyseRoute: String,
    loadRoute: String) {

  val play = make(playRoute)
  val analyse = make(analyseRoute)
  val load = make(loadRoute)

  private def make(route: String) = route.replace("{remote}", url)
}

private[ai] object Router {

  def apply(
    playRoute: String,
    analyseRoute: String,
    loadRoute: String)(url: String): Router =
    new Router(url, playRoute, analyseRoute, loadRoute)
}
