package lila.team

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global

import reactivemongo.api.MongoDriver

import lila.db.JsTubeInColl
import lila.db.Types.Coll

import acolyte.reactivemongo.{
  AcolyteDSL, 
  QueryResponse, 
  QueryResponseMaker, 
  PreparedResponse, 
  Request => Req, 
  WriteOp
}

private[team] trait RepoTest {
  type Repo
  def colName: String
  def repo(coll: Coll): Repo

  def withRepo[T](d: => MongoDriver)(f: Repo => Future[T]): Future[T] = 
    AcolyteDSL.withFlatCollection(d, colName) { coll => f(repo(coll)) }

  def withQueryHandler[T](h: Req => PreparedResponse)(f: Repo => Future[T]): Future[T] = AcolyteDSL.withFlatQueryHandler[T](h) { withRepo(_)(f) }

  def withQueryResult[A, B](res: => A)(f: Repo => Future[B])(implicit mk: QueryResponseMaker[A]): Future[B] = 
    withQueryHandler({ _: Req => QueryResponse(res) })(f)

  def withWriteHandler[T](h: (WriteOp, Req) => PreparedResponse)(f: Repo => Future[T]): Future[T] = AcolyteDSL.withFlatWriteHandler[T](h) { withRepo(_)(f) }
}
