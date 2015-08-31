package lila.search

import com.sksamuel.elastic4s.{ CountDefinition, SearchDefinition }

trait Query {

  def searchDef(from: Int = 0, size: Int = 10): SearchDefinition

  def countDef: CountDefinition
}
