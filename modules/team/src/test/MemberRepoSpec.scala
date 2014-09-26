package lila.team

import reactivemongo.bson.{ BSONDocument, BSONString, BSONDateTime }
import acolyte.reactivemongo.{
  CountRequest,
  DeleteOp, 
  QueryResponse, 
  InsertOp,
  Request => Req, 
  SimpleBody,
  ValueDocument, 
  WriteResponse
}

object MemberRepoSpec extends org.specs2.mutable.Specification 
    with MemberRepoTest with MemberRepoFixtures {

  "Member repository" title

  "User IDs by team" should {
    "be properly extracted by sending expected query" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(
          ("team", BSONString("anyTeamId1")) :: Nil)) =>
        // used query matches expected one, go on checking result processing
          QueryResponse(memberBson1)
      })(_ userIdsByTeam "anyTeamId1").
        aka("user IDs") must beEqualTo(List("userId1")).await(5)
    }

    "not be found" in {
      withQueryResult(List.empty[BSONDocument])(_ userIdsByTeam "anyTeamId1").
        aka("user IDs") must beEqualTo(Nil).await(5)
    }
  }

  "Team IDs by user" should {
    "be properly extracted" in {
      withQueryHandler(_ match {
        case Req(_, SimpleBody(
          ("user", BSONString("anyUserId2")) :: Nil)) =>
          // used query matches expected one, go on checking result processing
          QueryResponse(List(memberBson1, memberBson2))
      })(_ teamIdsByUser "anyUserId2") aka "team IDs" must beEqualTo(
        List("teamId1", "2teamId")).await(5)
    }

    "not be found" in {
      withQueryResult(List.empty[BSONDocument])(_ teamIdsByUser "anyUserId4").
        aka("user IDs") must beEqualTo(Nil).await(5)
    }
  }

  "Member" should {
    "exist (according expected query)" in {
      withQueryHandler(_ match {
        case CountRequest((_, ("_id", BSONString("userId3@teamId3")) :: Nil)) =>
          /* used query matched expected one */ QueryResponse.count(1)
      })(_ exists ("teamId3", "userId3")) aka "existence" must beTrue.await(5)
    }

    "not exist" in {
      withQueryResult(BSONDocument("ok" -> 1, "n" -> 0))(
        _ exists ("teamId4", "userId3")) aka "existence" must beFalse.await(5)
    }

    "be added using expected command" in {
      withWriteHandler({
        case (InsertOp, Req(_, SimpleBody(
          ("team", BSONString("anyTeamId4")) ::
            ("user", BSONString("anyUserId4")) :: ("date", BSONDateTime(_)) ::
            ("_id", BSONString("anyUserId4@anyTeamId4")) :: Nil))) =>
          // write operation is expected one
          WriteResponse(1)
        case cmd => WriteResponse.failed(s"Unexpected command used: $cmd")
      })(_ add ("anyTeamId4", "anyUserId4")).
        aka("creation") must beEqualTo(()).await(5)
    }
  }

  "Member removal" in {
    "be performed by team ID using expected command" in {
      withWriteHandler({ 
        case (DeleteOp, Req(_, SimpleBody(
          ("team", BSONString("anyTeamId2")) :: Nil))) => WriteResponse(1)
        case cmd => WriteResponse.failed(s"Unexpected command used: $cmd")
      })(_ removeByteam "anyTeamId2") aka "removal" must beEqualTo(()).await(5)
    }

    "be performed by user ID using expected command" in {
      withWriteHandler({ 
        case (DeleteOp, Req(_, SimpleBody(
          ("user", BSONString("anyUserId2")) :: Nil))) => WriteResponse(1)
        case cmd => WriteResponse.failed(s"Unexpected command used: $cmd")
      })(_ removeByUser "anyUserId2") aka "removal" must beEqualTo(()).await(5)
    }

    "be performed by either team ID and user ID using expected command" in {
      withWriteHandler({
        case (DeleteOp, Req(_, SimpleBody((
          "_id", BSONString("anyUserId3@anyTeamId3")) :: Nil))) =>
          WriteResponse(1)
        case cmd => WriteResponse.failed(s"Unexpected command used: $cmd")
      })(_ remove ("anyTeamId3", "anyUserId3")).
        aka("removal") must beEqualTo(()).await(5)
    }
  }
}

sealed trait MemberRepoTest extends RepoTest {
  import lila.db.JsTubeInColl
  import lila.db.Types.Coll

  type Repo = MemberRepo
  val colName = "members"
  def repo(coll: Coll): Repo = TestRepo(Member.tube inColl coll)

  private case class TestRepo(inColl: JsTubeInColl[Member]) extends MemberRepo
}

sealed trait MemberRepoFixtures {
  import org.joda.time.DateTime

  val date1 = DateTime.now

  val memberBson1 = BSONDocument(
    "id" -> "memberId1", "team" -> "teamId1",
    "user" -> "userId1", "date" -> BSONDateTime(date1.getMillis))

  val date2 = DateTime.now

  val memberBson2 = BSONDocument(
    "id" -> "2memberId", "team" -> "2teamId",
    "user" -> "2userId", "date" -> BSONDateTime(date2.getMillis))
}
