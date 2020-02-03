package com.nulabinc.backlog.migration.common.persistence.sqlite

import com.nulabinc.backlog.migration.common.domain.Types.AnyId
import slick.dbio.Effect.{All, Read, Write}
import slick.dbio.{DBIOAction, NoStream, StreamingDBIO}

// https://scala-slick.org/doc/3.3.2/dbio.html
object DBIOTypes {
  type DBIORead[X]   = DBIOAction[X, NoStream, Read]
  type DBIOWrite     = DBIOAction[AnyId, NoStream, Write]
  type DBIOWrites    = DBIOAction[Seq[AnyId], NoStream, All]
  type DBIOStream[A] = StreamingDBIO[Seq[A], A]
}