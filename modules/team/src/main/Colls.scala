package lidraughts.team

import lidraughts.db.dsl.Coll

private final class Colls(
    val team: Coll,
    val request: Coll,
    val member: Coll
)
