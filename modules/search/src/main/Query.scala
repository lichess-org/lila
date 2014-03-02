package lila.search

import com.sksamuel.elastic4s.ElasticDsl._

trait Query {

  def searchDef(from: Int = 0, size: Int = 10): SearchDefinition

  def countDef: CountDefinition
}
