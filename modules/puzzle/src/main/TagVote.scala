package lila.puzzle

case class TagVote(
    _id: String, // puzzleId/tagId/userId
    v: Boolean) {

    def id = _id

    def value = v
}

object TagVote {

    def makeId(puzzleId: PuzzleId, tagId: String, userId: String) = s"$puzzleId/$tagId/$userId"

    implicit val tagVoteBSONHandler = reactivemongo.bson.Macros.handler[TagVote]
}
