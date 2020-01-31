package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.Types.AnyId
import com.nulabinc.backlog.migration.common.dsl.DBIOTypes.{DBIORead, DBIOStream, DBIOWrite}
import monix.reactive.Observable
import slick.dbio.Effect.{All, Read, Write}
import slick.dbio.{DBIOAction, NoStream, StreamingDBIO}

trait StoreDSL[F[_]] {
  def read[A](a: DBIORead[A]): F[A]
  def write[A](a: DBIOWrite): F[Int]
  def stream[A](a: DBIOStream[A]): F[Observable[A]]
}

// https://scala-slick.org/doc/3.3.2/dbio.html
object DBIOTypes {
  type DBIORead[X]   = DBIOAction[X, NoStream, Read]
  type DBIOWrite     = DBIOAction[Int, NoStream, Write]
  type DBIOWrites    = DBIOAction[Seq[AnyId], NoStream, All]
  type DBIOStream[A] = StreamingDBIO[Seq[A], A]
}