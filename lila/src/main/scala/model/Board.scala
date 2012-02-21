package lila
package model

//case class Board(pieces: Map[Pos, Piece], taken: List[Piece]) {

  //def this() = this(Map(), Nil)

  /**
   * Place a piece on the board (at a position)
   * @return a new board
   */
  //def place(p: Piece) = {
    //case class Placement() {
      //def at(s: Symbol): Board = at(position(s))

      //def at(destination: Pos): Board = new Board(pieces(destination) = p, taken)
    //}
    //new Placement
  //}

  /**
   * Take (capture) the piece at the given position
   * @return a new board
   */
  //def take(p: Pos) = new Board((pieces - p), (pieces(p) :: taken))

  /**
   * Move the piece at the given position (to a new position)
   * @return a new board
   */
  //def move(orig: Pos) = {
    //case class Movement() {
      //def to(dest: Pos) = {
        //if (pieces contains dest) throw new IllegalMoveException("Cannot move to occupied " + dest)
        //else pieces.get(orig).map(piece => new Board((pieces - orig)(dest) = piece, taken))
                //.getOrElse {throw new IllegalMoveException("No piece at " + orig + " to move")}
      //}
    //}
    //new Movement
  //}

  /**
   * Promote the piece at the given position to a new role
   * @return a new board
   */
  //def promote(p: Pos) = {
    //case class Promotion() {
      //def to(r: Role): Board = r match {
        //case King => throw new IllegalPromotionException("Cannot promote to King")
        //case _ => pieces.get(p).map(piece => piece.role match {
          //case Pawn => new Board(pieces(p) = Piece(piece.colour, r), taken)
          //case _ => throw new IllegalPromotionException("Can only promote pawns")
        //}).getOrElse {throw new IllegalPromotionException("No piece at " + p + " to promote")}
      //}
    //}
    //new Promotion
  //}

  /**
   * Finds all positions which contain a threat to the given colour (at a given position).
   * Note, will not identify threats to position from en passant (as this behaviour is not required).
   * @return the positions of threat
   */
  //def threatsTo(colour: Colour) = {
    //case class Threat() {
      //def at(s: Symbol): List[Pos] = at(position(s))

      //def at(pos: Pos): List[Pos] = {
        //def expand(direction: Seq[Option[Pos] => Option[Pos]]): Option[Pos] = {
          //def next(from: Option[Pos]): Option[Pos] = {
            //val nextPose = direction.foldLeft(from) {(acc, next) => next(acc)}
            //if (nextPose.isEmpty) return None
            //if (pieces.contains(nextPose.get)) Some(nextPose.get) else next(nextPose)
          //}
          //next(Some(pos))
        //}

        //def opposing(r: Role) = {
          //case class OpposingRoleCheck() {
            //def at(p: Pos) = pieces.get(p).map(_.equals(Piece(opposite of colour, r))).getOrElse(false)
          //}
          //new OpposingRoleCheck
        //}

        //val forward = if (White.equals(colour)) ^ _ else v _
        //val pawns: List[Pos] = Set(forward(pos < 1), forward(pos > 1)).filter(_.isDefined).foldLeft(Nil: List[Pos]) {
          //(acc, next) =>
                  //if (opposing(Pawn).at(next.get)) next.get :: acc else acc
        //}
        //val rankFileVectors: List[Pos] = (expand(Seq(< _)) :: expand(Seq(^ _)) :: expand(Seq(> _)) :: expand(Seq(v _)) :: Nil)
                //.filter(_.isDefined).foldLeft(Nil: List[Pos]) {
          //(acc, next) =>
                  //if (opposing(Rook).at(next.get) || opposing(Queen).at(next.get)) next.get :: acc else acc
        //}
        //val diagonalVectors: List[Pos] = (expand(Seq(< _, ^ _)) :: expand(Seq(> _, ^ _)) :: expand(Seq(> _, v _)) :: expand(Seq(< _, v _)) :: Nil)
                //.filter(_.isDefined).foldLeft(Nil: List[Pos]) {
          //(acc, next) =>
                  //if (opposing(Bishop).at(next.get) || opposing(Queen).at(next.get)) next.get :: acc else acc
        //}
        //val knights: List[Pos] = {
          //val options = radialBasedPoss(pos, List(-2, -1, 1, 2), (rank, file) => Math.abs(rank) != Math.abs(file))
          //options.toList.filter(opposing(Knight).at(_))
        //}
        //val kings: List[Pos] = {
          //val options = radialBasedPoss(pos, -1 to 1, (rank, file) => (rank != 0 || file != 0))
          //options.toList.filter(opposing(King).at(_))
        //}

        //pawns ::: rankFileVectors ::: diagonalVectors ::: knights ::: kings
      //}
    //}
    //new Threat
  //}

  /**
   * Layout the board for a new game.
   * @return a new board
   */
  //def reset = {
    //val lineUp = Rook :: Knight :: Bishop :: Queen :: King :: Bishop :: Knight :: Rook :: Nil
    //val pairs = for (rank <- 1 :: 2 :: 7 :: 8 :: Nil; file <- 1 to 8) yield (position(rank, file),
            //rank match {
              //case 1 => Piece(White, lineUp(file - 1))
              //case 2 => Piece(White, Pawn)
              //case 7 => Piece(Black, Pawn)
              //case 8 => Piece(Black, lineUp(file - 1))
            //})
    //new Board(pairs.foldLeft(Map.empty[Pos, Piece]) {(acc, next) => acc(next._1) = next._2}, Nil)
  //}
//}
