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

import scala.annotation.tailrec
import scala.util.Try

// format: off
object WMMatching {

  def apply[A](vertices: Array[A], pairScore: (A, A) => Option[Int]): Try[List[(A, A)]] = Try {
    lowLevel(
      vertices.length,
      (i, j) => pairScore(vertices(i), vertices(j))
    ) map {
        case (i, j) => vertices(i) -> vertices(j)
      }
  }

  private def lowLevel(nvertex: Int, pairScore: (Int, Int) => Option[Int]): List[(Int, Int)] = {
    val (endpoint, weights) = fullGraph(nvertex, pairScore)
    if (endpoint.isEmpty) Nil
    else minWeightMatching(endpoint, weights)
  }

  private def maxWeightMatching(endpoint: Array[Int], weights: Array[Int], maxcardinality: Boolean): List[(Int, Int)] = {
    /*
    Compute a maximum-weighted matching in the general undirected
    weighted graph given by "edges".  If "maxcardinality" is true,
    only maximum-cardinality matchings are considered as solutions.

    Edges is a sequence of tuples (i, j, wt) describing an undirected
    edge between vertex i and vertex j with weight wt.  There is at most
    one edge between any two vertices; no vertex has an edge to itself.
    Vertices are identified by consecutive, non-negative integers.

    Return a list "mate", such that mate(i) == j if vertex i is
    matched to vertex j, and mate(i) == -1 if vertex i is not matched.

    This function takes time O(n ** 3).

    Vertices are numbered 0 .. (nvertex-1).
    Non-trivial blossoms are numbered nvertex .. (2*nvertex-1)

    Edges are numbered 0 .. (nedge-1).
    Edge endpoints are numbered 0 .. (2*nedge-1), such that endpoints
    (2*k) and (2*k+1) both belong to edge k.

    Many terms used in the comments (sub-blossom, T-vertex) come from
    the paper by Galil; read the paper before reading this code.

     */
    val nedge = weights.length
    val nvertex = 1 + endpoint.max
    // Find the maximum edge weight.
    val maxweight = weights.max
    //If v is a vertex,
    //neighbend(v) is the list of remote endpoints of the edges attached to v.
    val neighbend: Array[List[Int]] = Array.fill(nvertex)(Nil)
    endpoint.zipWithIndex.reverseIterator.foreach { p => neighbend(p._1) ::= p._2 ^ 1 }

    // If v is a vertex,
    // mate(v) is the remote endpoint of its matched edge, or -1 if it is single
    // (i.e. endpoint(mate(v)) is v's partner vertex).
    // Initially all vertices are single; updated during augmentation.
    val mate: Array[Int] = Array.fill(nvertex)(-1)

    /*
    If b is a top-level blossom,
    label(b) is 0 if b is unlabeled (free);
                1 if b is an S-vertex/blossom;
                2 if b is a T-vertex/blossom.
    The label of a vertex is found by looking at the label of its
    top-level containing blossom.
    If v is a vertex inside a T-blossom,
    label(v) is 2 iff v is reachable from an S-vertex outside the blossom.
    Labels are assigned during a stage and reset after each augmentation.
     */
    val label: Array[Byte] = new Array[Byte](2 * nvertex)
    /*
    If b is a labeled top-level blossom,
    labelend(b) is the remote endpoint of the edge through which b obtained
    its label, or -1 if b's base vertex is single.
    If v is a vertex inside a T-blossom and label(v) == 2,
    labelend(v) is the remote endpoint of the edge through which v is
    reachable from outside the blossom.
     */
    val labelend: Array[Int] = Array.fill(2 * nvertex)(-1)

    /*
    If v is a vertex, inblossom(v) is the top-level blossom to which v belongs.
    If v is a top-level vertex, v is itself a blossom (a trivial blossom)
    and inblossom(v) == v.
    Initially all vertices are top-level trivial blossoms.
     */
    val inblossom: Array[Int] = Array.range(0, nvertex)

    /*
    If b is a sub-blossom,
    blossomparent(b) is its immediate parent (sub-)blossom.
    If b is a top-level blossom, blossomparent(b) is -1.
     */
    val blossomparent: Array[Int] = Array.fill(2 * nvertex)(-1)
    /*
    If b is a non-trivial (sub-)blossom,
    blossomchilds(b) is an ordered list of its sub-blossoms, starting with
    the base and going round the blossom.
     */
    val blossomchilds: Array[Array[Int]] = new Array(2 * nvertex)

    // If b is a (sub-)blossom,
    // blossombase(b) is its base VERTEX (i.e. recursive sub-blossom).
    val blossombase: Array[Int] = Array.tabulate(2 * nvertex) { i: Int => if (i < nvertex) i else -1 }

    /*
    If b is a non-trivial (sub-)blossom,
    blossomendps(b) is a list of endpoints on its connecting edges,
    such that blossomendps(b)(i) is the local endpoint of blossomchilds(b)(i)
    on the edge that connects it to blossomchilds(b)(wrap(i+1)).
     */
    val blossomendps: Array[Array[Int]] = new Array(2 * nvertex)

    /*
    If v is a free vertex (or an unreached vertex inside a T-blossom),
    bestedge(v) is the edge to an S-vertex with least slack,
    or -1 if there is no such edge.
    If b is a (possibly trivial) top-level S-blossom,
    bestedge(b) is the least-slack edge to a different S-blossom,
    or -1 if there is no such edge.
    This is used for efficient computation of delta2 and delta3.
     */
    val bestedge: Array[Int] = Array.fill(2 * nvertex)(-1)

    // If b is a non-trivial top-level S-blossom,
    // blossombestedges(b) is a list of least-slack edges to neighbouring
    // S-blossoms, or None if no such list has been computed yet.
    // This is used for efficient computation of delta3.
    val blossombestedges: Array[List[Int]] = new Array(2 * nvertex)

    // List of currently unused blossom numbers.
    var unusedblossoms = List.range(nvertex, 2 * nvertex)
    var allocatedvertex = nvertex

    // If v is a vertex,
    // dualvar(v) = 2 * u(v) where u(v) is the v's variable in the dual
    // optimization problem (multiplication by two ensures integer values
    // throughout the algorithm if all edge weights are integers).
    // If b is a non-trivial blossom,
    // dualvar(b) = z(b) where z(b) is b's variable in the dual optimization
    // problem.
    val dualvar: Array[Int] = Array.tabulate(2 * nvertex) { i: Int => if (i < nvertex) maxweight else 0 }

    // If allowedge(k) is true, edge k has zero slack in the optimization
    // problem; if allowedge(k) is false, the edge's slack may or may not
    // be zero.

    val allowedge = new Array[Boolean](nedge)
    var queue: List[Int] = Nil

    //slack function optimization
    for (i <- 0 until nedge) weights(i) *= 2

    // Return 2 * slack of edge k (does not work inside blossoms).
    def slack(k: Int) = {
      val kk = 2 * k
      dualvar(endpoint(kk)) + dualvar(endpoint(kk+1)) - weights(k)
    }

    def blossomLeavesG(v: Int): Iterator[Int] = {
      blossomchilds(v).iterator.flatMap { w =>
        if(w < nvertex) Iterator(w) else blossomLeavesG(w)
      }
    }

    // Generate the leaf vertices of a blossom.
    def blossomLeaves(b: Int): Iterator[Int] = {
      if(b < nvertex) Iterator(b) else blossomLeavesG(b)
    }

    // Assign label t to the top-level blossom containing vertex w
    // and record the fact that w was reached through the edge with
    // remote endpoint p.

    @tailrec def assignLabel(w: Int, t: Byte, p: Int): Unit = {
      val b = inblossom(w)
      label(w) = t
      label(b) = t
      labelend(w) = p
      labelend(b) = p
      bestedge(w) = -1
      bestedge(b) = -1
      if (t == 1) {
        //b became an S-vertex/blossom; add it(s vertices) to the queue.
        blossomLeaves(b).foreach(queue ::= _)
      } else {
        assert (t == 2)
        // b became a T-vertex/blossom; assign label S to its mate.
        // (If b is a non-trivial blossom, its base is the only vertex
        // with an external mate.)
        val mate_base = mate(blossombase(b))
        assignLabel(endpoint(mate_base), 1, mate_base ^ 1)
      }
    }
    // Trace back from vertices v and w to discover either a new blossom
    // or an augmenting path. Return the base vertex of the new blossom or -1.
    def scanBlossom(v: Int, w: Int) = {
      // Trace back from v and w, placing breadcrumbs as we go.
      @tailrec def scan(v: Int, w: Int, path: List[Int]): (Int, List[Int]) = {
        if (v == -1 && w == -1) (-1, path) //not found
        else {
          // Look for a breadcrumb in v's blossom or put a new breadcrumb.
          val b = inblossom(v)
          if (label(b) == 3) (blossombase(b), path)
          else {
            val path2 = b :: path
            label(b) = 3
            // Trace one step back.
            val nv =
              if (labelend(b) == -1) -1
              else {
                val t = endpoint(labelend(b))
                val c = inblossom(t)
                // c is a T-blossom; trace one more step back.
                endpoint(labelend(c))
              }
            // Swap v and w so that we alternate between both paths.
            if (w != -1) scan(w, nv, path2)
            else scan(nv, w, path2)
          }
        }
      }
      val (base, p) = scan(v, w, Nil)
      // Remove breadcrumbs.
      p.foreach(b => label(b) = 1)
      base
    }
    /*
    Construct a new blossom with given base, containing edge k which
    connects a pair of S vertices. Label the new blossom as S; set its dual
    variable to zero; relabel its T-vertices to S and add them to the queue.
     */
    def addBlossom(base: Int, k: Int) = {
      val v = endpoint(2*k)
      val w = endpoint(2*k+1)
      val bb = inblossom(base)
      // Create blossom.
      val b = unusedblossoms.head
      unusedblossoms = unusedblossoms.tail
      if (allocatedvertex <= b) {
        allocatedvertex = b + 1
      }

      blossombase(b) = base
      blossomparent(b) = -1
      blossomparent(bb) = b
      // Make list of sub-blossoms and their interconnecting edge endpoints.

      @tailrec def traceBack(v: Int, d: Int, path: List[Int], endps: List[Int]): (List[Int], List[Int]) = {
        val bv = inblossom(v)
        if (bv == bb) (path, endps)
        else {
          // Add bv to the new blossom.
          blossomparent(bv) = b
          // Trace one step back.
          traceBack(endpoint(labelend(bv)), d, bv :: path, (labelend(bv) ^ d) :: endps)
        }
      }
      // Trace back from w to base.
      val (path2, endps2) = traceBack(w, 1, Nil, Nil)
      // Trace back from v to base.
      val (path1, endps1) = traceBack(v, 0, path2.reverse, (2 * k) :: (endps2.reverse))
      blossomchilds(b) = (bb :: path1).toArray
      blossomendps(b) = endps1.toArray

      // Set label to S.
      label(b) = 1
      labelend(b) = labelend(bb)
      // Set dual variable to zero.
      dualvar(b) = 0
      // Relabel vertices.
      for (v <- blossomLeaves(b)) {
        if (label(inblossom(v)) == 2) {
          // This T-vertex now turns into an S-vertex because it becomes
          // part of an S-blossom; add it to the queue.
          queue ::= v
        }
        inblossom(v) = b
      }
      // Compute blossombestedges(b).

      val bestedgeto = Array.fill(allocatedvertex)(-1)
      for (bv <- blossomchilds(b)) {
        val nblists =
          if (blossombestedges(bv) == null) blossomLeaves(bv).flatMap(v => neighbend(v).view.map { p => p >> 1 })
          else blossombestedges(bv).iterator
        for (k <- nblists) {
          val kk = 2 * k
          val j = if (inblossom(endpoint(kk+1)) == b) endpoint(kk) else endpoint(kk+1)
          val bj = inblossom(j)
          if (bj != b && label(bj) == 1 && (bestedgeto(bj) == -1 || slack(k) < slack(bestedgeto(bj)))) bestedgeto(bj) = k
        }
        // Forget about least-slack edges of the subblossom.
        blossombestedges(bv) = null
        bestedge(bv) = -1
      }
      blossombestedges(b) = bestedgeto.view.filter { k => k != -1 }.toList
      // Select bestedge(b).
      bestedge(b) = -1
      for (k <- blossombestedges(b)) {
        if (bestedge(b) == -1 || slack(k) < slack(bestedge(b)))
          bestedge(b) = k
      }
    }

    // Expand the given top-level blossom.
    def expandBlossom(b: Int, endstage: Boolean): Unit = {
      // Convert sub-blossoms into top-level blossoms.
      for (s <- blossomchilds(b)) {
        blossomparent(s) = -1
        if (s < nvertex)
          inblossom(s) = s
        else if (endstage && dualvar(s) == 0)
          //Recursively expand this sub-blossom.
          expandBlossom(s, endstage)
        else
          blossomLeaves(s).foreach(v => inblossom(v) = s)
      }
      // If we expand a T-blossom during a stage, its sub-blossoms must be
      // relabeled.
      if (!endstage && label(b) == 2) {
        // Start at the sub-blossom through which the expanding
        // blossom obtained its label, and relabel sub-blossoms until
        // we reach the base.
        // Figure out through which sub-blossom the expanding blossom
        // obtained its label initially.
        val entrychild = inblossom(endpoint(labelend(b) ^ 1))
        // Decide in which direction we will go round the blossom.
        var j = blossomchilds(b).indexOf(entrychild)
        val l1 = blossomchilds(b).length - 1
        val (jstep, endptrick) =
          // Start index is odd; go forward and wrap.
          if ((j & 1) != 0) ((j: Int) => { if (j == l1) 0 else (j + 1) }, 0)
          // Start index is even; go backward.
          else ((j: Int) => { if (j == 0) l1 else (j - 1) }, 1)
        // Move along the blossom until we get to the base.
        var p = labelend(b)
        while (j != 0) {
          //Relabel the T-sub-blossom.
          label(endpoint(p ^ 1)) = 0
          label(endpoint(blossomendps(b)(j - endptrick) ^ endptrick ^ 1)) = 0
          assignLabel(endpoint(p ^ 1), 2, p)
          //Step to the next S-sub-blossom and note its forward endpoint.
          allowedge(blossomendps(b)(j - endptrick) >> 1) = true
          j = jstep(j)
          p = blossomendps(b)(j - endptrick) ^ endptrick
          // Step to the next T-sub-blossom.
          allowedge(p >> 1) = false
          j = jstep(j)
        }
        // Relabel the base T-sub-blossom WITHOUT stepping through to
        // its mate (so don't call assignLabel).
        val bv = blossomchilds(b)(j)
        label(endpoint(p ^ 1)) = 2
        label(bv) = 2
        labelend(endpoint(p ^ 1)) = p
        labelend(bv) = p
        bestedge(bv) = -1
        // Continue along the blossom until we get back to entrychild.
        j = jstep(j)
        while (blossomchilds(b)(j) != entrychild) {
          // Examine the vertices of the sub-blossom to see whether
          // it is reachable from a neighbouring S-vertex outside the
          // expanding blossom.
          val bv = blossomchilds(b)(j)
          if (label(bv) == 1) {
            // This sub-blossom just got label S through one of its
            // neighbours; leave it.
          } else {
            (blossomLeaves(bv).find(x => label(x) != 0)).foreach(v => {
              label(v) = 0
              label(endpoint(mate(blossombase(bv)))) = 0
              assignLabel(v, 2, labelend(v))
            })
          }
          j = jstep(j)
        }
      }
      // Recycle the blossom number.
      label(b) = -1
      labelend(b) = -1
      blossomchilds(b) = null
      blossomendps(b) = null
      blossombase(b) = -1
      blossombestedges(b) = null
      bestedge(b) = -1
      unusedblossoms ::= b
      if (b + 1 == allocatedvertex) {
        allocatedvertex -= 1
      }
    }

    // Swap matched/unmatched edges over an alternating path through blossom b
    // between vertex v and the base vertex. Keep blossom bookkeeping consistent.
    def augmentBlossom(b: Int, v: Int): Unit = {
      // Bubble up through the blossom tree from vertex v to an immediate
      // isub-blossom of b.
      def rotate(src: Array[Int], dst: Array[Int], n: Int, shift: Int): Unit = {
        Array.copy(src, shift, dst, 0, n - shift)
        Array.copy(src, 0, dst, n - shift, shift)
      }
      var t = v
      while (blossomparent(t) != b) {
        t = blossomparent(t)
      }
      // Recursively deal with the first sub-blossom.
      if (t >= nvertex)
        augmentBlossom(t, v)
      // Decide in which direction we will go round the blossom.
      val l1 = blossomchilds(b).length - 1
      val i = blossomchilds(b).indexOf(t)
      var j = i
      val (jstep, endptrick) =
        if ((j & 1) != 0) ((j: Int) => { if (j == l1) 0 else (j + 1) }, 0)
        else ((j: Int) => { if (j == 0) l1 else (j - 1) }, 1)
      // Move along the blossom until we get to the base.
      while (j != 0) {
        // Step to the next sub-blossom and augment it recursively.
        j = jstep(j)
        t = blossomchilds(b)(j)
        val p = blossomendps(b)(j - endptrick) ^ endptrick
        if (t >= nvertex)
          augmentBlossom(t, endpoint(p))
        // Step to the next sub-blossom and augment it recursively.
        j = jstep(j)
        t = blossomchilds(b)(j)
        if (t >= nvertex)
          augmentBlossom(t, endpoint(p ^ 1))
        // Match the edge connecting those sub-blossoms.
        mate(endpoint(p)) = p ^ 1
        mate(endpoint(p ^ 1)) = p
      }
      // Rotate the list of sub-blossoms to put the new base at the front.
      if(i > 0) {
        val n = blossomchilds(b).length
        val t1 = new Array[Int](n)
        rotate(blossomchilds(b), t1, n, i)
        val t2 = blossomchilds(b)
        blossomchilds(b) = t1
        rotate(blossomendps(b), t2, n, i)
        blossomendps(b) = t2
      }
      blossombase(b) = blossombase(blossomchilds(b)(0))
    }

    // Swap matched/unmatched edges over an alternating path between two
    // single vertices. The augmenting path runs through edge k, which
    // connects a pair of S vertices.
    def augmentMatching(k: Int): Unit = {
      val kk = 2 * k
      val v = endpoint(kk)
      val w = endpoint(kk+1)
      // Match vertex s to remote endpoint p. Then trace back from s
      // until we find a single vertex, swapping matched and unmatched
      // edges as we go.
      @tailrec def f(s: Int, p: Int): Unit = {
        val bs = inblossom(s)
        // Augment through the S-blossom from s to base.
        if (bs >= nvertex)
          augmentBlossom(bs, s)
        mate(s) = p
        // Trace one step back.
        if (labelend(bs) == -1) {
          //Reached single vertex; stop.
        } else {
          val t = endpoint(labelend(bs))
          val bt = inblossom(t)
          // Trace one step back.
          val ns = endpoint(labelend(bt))
          val j = endpoint(labelend(bt) ^ 1)
          //Augment through the T-blossom from j to base.
          if (bt >= nvertex)
            augmentBlossom(bt, j)
          //Update mate(j)
          mate(j) = labelend(bt)
          // Keep the opposite endpoint;
          // it will be assigned to mate(s) in the next step.
          val np = labelend(bt) ^ 1
          f(ns, np)
        }
      }
      f(v, 2 * k + 1)
      f(w, 2 * k)
    }
    @tailrec def substage(): Boolean = {
      if (queue.isEmpty) false
      else {
        // Take an S vertex from the queue.
        val v = queue.head
        queue = queue.tail
        def go(p: Int): Boolean = {
          val k = p >> 1
          val w = endpoint(p)
          // w is a neighbour to v
          if (inblossom(v) == inblossom(w)) {
            //this edge is internal to a blossom; ignore it
            false
          } else {
            var kslack = 0
            if (!allowedge(k)) {
              kslack = slack(k)
              if (kslack <= 0) {
                allowedge(k) = true
              }
            }
            if (allowedge(k)) {
              if (label(inblossom(w)) == 0) {
                // (C1) w is a free vertex;
                // label w with T and label its mate with S (R12).
                assignLabel(w, 2, p ^ 1)
                false
              } else if (label(inblossom(w)) == 1) {
                // (C2) w is an S-vertex (not in the same blossom);
                // follow back-links to discover either an
                // augmenting path or a new blossom.
                val base = scanBlossom(v, w)
                if (base >= 0) {
                  // Found a new blossom; add it to the blossom
                  // bookkeeping and turn it into an S-blossom.
                  addBlossom(base, k)
                  false
                } else {
                  // Found an augmenting path; augment the
                  // matching and end this stage.
                  augmentMatching(k)
                  true
                }
              } else if (label(w) == 0) {
                // w is inside a T-blossom, but w itself has not
                // yet been reached from outside the blossom;
                // mark it as reached (we need this to relabel
                // during T-blossom expansion).
                label(w) = 2
                labelend(w) = p ^ 1
                false
              } else {
                false
              }
            } else if (label(inblossom(w)) == 1) {
              // keep track of the least-slack non-allowable edge to
              // a different S-blossom.
              val b = inblossom(v)
              if (bestedge(b) == -1 || kslack < slack(bestedge(b))) {
                bestedge(b) = k
              }
              false
            } else if (label(w) == 0) {
              // w is a free vertex (or an unreached vertex inside
              // a T-blossom) but we can not reach it yet;
              // keep track of the least-slack edge that reaches w.
              if (bestedge(w) == -1 || kslack < slack(bestedge(w))) {
                bestedge(w) = k
              }
              false
            } else {
              false
            }
          }
        }
        if (neighbend(v).exists(go)) {
          true
        } else {
          substage()
        }
      }
    }

    def updateDual(): Boolean = {
      // There is no augmenting path under these constraints;
      // compute delta and reduce slack in the optimization problem.
      // (Note that our vertex dual variables, edge slacks and delta's
      // are pre-multiplied by two.)

      var dt_tp: Byte = -1
      var dt_delta = Int.MaxValue
      var dt_extra = -1
      def dt_update(t: Byte, d: Int, e: Int) = {
        if (dt_delta > d) {
          dt_tp = t
          dt_delta = d
          dt_extra = e
        }
      }

      // Compute delta1: the minimum value of any vertex dual.
      if (!maxcardinality) {
        dt_update(1, dualvar.slice(0, nvertex).min, -1)
      }

      // Compute delta2: the minimum slack on any edge between
      // an S-vertex and a free vertex.

      for (v <- 0 until nvertex) {
        if (label(inblossom(v)) == 0 && bestedge(v) != -1) {
          dt_update(2, slack(bestedge(v)), bestedge(v))
        }
      }

      // Compute delta3: half the minimum slack on any edge between
      // a pair of S-blossoms.
      for (b <- 0 until allocatedvertex) {
        if (blossomparent(b) == -1 && label(b) == 1 && bestedge(b) != -1) {
          val kslack = slack(bestedge(b))
          dt_update(3, kslack / 2, bestedge(b))
        }
      }

      // Compute delta4: minimum z variable of any T-blossom.
      for (b <- nvertex until allocatedvertex) {
        if (blossombase(b) >= 0 && blossomparent(b) == -1 && label(b) == 2) {
          dt_update(4, dualvar(b), b)
        }
      }

      if (dt_tp == -1) {
        // No further improvement possible; max-cardinality optimum
        // reached. Do a final delta update to make the optimum
        // verifyable.
        dt_update(1, 0.max(dualvar.slice(0, nvertex).min), -1)
      }

      // Update dual variables according to delta.
      for (v <- 0 until nvertex) {
        if (label(inblossom(v)) == 1) {
          // S-vertex: 2*u = 2*u - 2*delta
          dualvar(v) -= dt_delta
        } else if (label(inblossom(v)) == 2) {
          //T-vertex: 2*u = 2*u + 2*delta
          dualvar(v) += dt_delta
        }
      }

      for (b <- nvertex until allocatedvertex) {
        if (blossombase(b) >= 0 && blossomparent(b) == -1) {
          if (label(b) == 1)
            dualvar(b) += dt_delta
          else if (label(b) == 2)
            dualvar(b) -= dt_delta
        }
      }

      // Take action at the point where minimum delta occurred.
      if (dt_tp == 1) {
        // No further improvement possible; optimum reached.
      } else if (dt_tp == 2) {
        // Use the least-slack edge to continue the search.
        allowedge(dt_extra) = true
        val kk = 2 * dt_extra
        val ei = endpoint(kk)
        val i = if (label(inblossom(ei)) == 0) endpoint(kk+1) else ei
        queue ::= i
      } else if (dt_tp == 3) {
        // Use the least-slack edge to continue the search.
        allowedge(dt_extra) = true
        queue ::= endpoint(2 * dt_extra)
      } else if (dt_tp == 4) {
        expandBlossom(dt_extra, false)
      }
      dt_tp > 1
    }

    // Main loop: continue until no further improvement is possible.
    @tailrec def mainLoop(iterations: Int): Unit = {
      @tailrec def stage(): Boolean = {
        if (substage()) true
        else if (!updateDual()) false
        else stage()
      }
      // Each iteration of this loop is a "stage".
      // A stage finds an augmenting path and uses that to improve
      // the matching.

      // Remove labels from top-level blossoms/vertices.
      java.util.Arrays.fill(label, 0, allocatedvertex, 0.toByte)

      // Forget all about least-slack edges.
      java.util.Arrays.fill(bestedge, 0, allocatedvertex, -1)
      for (k <- nvertex until allocatedvertex) {
        blossombestedges(k) = null
      }
      // Loss of labeling means that we can not be sure that currently
      // allowable edges remain allowable throughout this stage.
      java.util.Arrays.fill(allowedge, false)

      // Make queue empty.
      queue = Nil

      // Label single blossoms/vertices with S and put them in the queue.
      for (v <- 0 until nvertex) {
        if (mate(v) == -1 && label(inblossom(v)) == 0)
          assignLabel(v, 1, -1)
      }
      if (stage() && iterations > 1) {
        mainLoop(iterations - 1)
      }
    }
    mainLoop(nvertex)
    // Transform mate such that mate(v) is the vertex to which v is paired.
    (for (v <- 0 until nvertex
         if mate(v) >= 0
         if v < endpoint(mate(v))) yield (v, endpoint(mate(v)))).toList
  }

  private def minWeightMatching(endpoint: Array[Int], weights: Array[Int]): List[(Int, Int)] = {
    val maxweight = weights.max
    maxWeightMatching(endpoint, weights.map { x => maxweight - x }, true)
  }

  private def fullGraph(nvertex: Int, pairScore: (Int, Int) => Option[Int]): (Array[Int], Array[Int]) = {
    val m = nvertex * (nvertex - 1)
    val e = Array.newBuilder[Int]
    e.sizeHint (m)
    val w = Array.newBuilder[Int]
    e.sizeHint (m >> 1)
    for {i <- 0 until (nvertex - 1)
         j <- (i+1) until nvertex
         p <- pairScore(i, j)} {
      e += i
      e += j
      w += p
    }
    (e.result(), w.result())
  }
}
