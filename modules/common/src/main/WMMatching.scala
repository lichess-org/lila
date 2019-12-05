/*
This is port Joris van Rantwijk's Python code (http://jorisvr.nl/article/maximum-matching) to Scala language.

Weighted maximum matching in general graphs.

The algorithm is taken from "Efficient Algorithms for Finding Maximum
Matching in Graphs" by Zvi Galil, ACM Computing Surveys, 1986.
It is based on the "blossom" method for finding augmenting paths and
the "primal-dual" method for finding a matching of maximum weight, both
due to Jack Edmonds.
Some ideas came from "Implementation of algorithms for maximum matching
on non-bipartite graphs" by H.J. Gabow, Standford Ph.D. thesis, 1973.

A C program for maximum weight matching by Ed Rothberg was used extensively
to validate this new code.
*/

package lila.common

import scala.util.Try

object WMMatching {

  def apply[A](vertices: Array[A], pairScore: (A, A) => Option[Int]): Try[List[(A, A)]] = Try {
    lowLevel(
      vertices.length,
      (i, j) => pairScore(vertices(i), vertices(j))
    ) map {
        case (i, j) => vertices(i) -> vertices(j)
      }
  }

  // #TODO implement
  private def lowLevel(nvertex: Int, pairScore: (Int, Int) => Option[Int]): List[(Int, Int)] = Nil
}
