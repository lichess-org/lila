package lidraughts.team

import lidraughts.db.dsl.Coll

final class Colls(
    val team: Coll,
    val request: Coll,
    val member: Coll
)
