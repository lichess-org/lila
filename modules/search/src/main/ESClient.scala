package lila.search

trait ESClient {
}

object ESClient {

  def make: ESClient = new ESClient {}
}
