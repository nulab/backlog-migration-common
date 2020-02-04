package com.nulabinc.backlog.migration.common.persistence.sqlite

import slick.dbio.Effect.{Read, Write}
import slick.dbio.{DBIOAction, NoStream, StreamingDBIO}

// https://scala-slick.org/doc/3.3.2/dbio.html
object DBIOTypes {
  type DBIORead[X]   = DBIOAction[X, NoStream, Read]
  type DBIOWrite     = DBIOAction[Int, NoStream, Write]
  type DBIOWrites    = DBIOAction[Seq[Int], NoStream, Write]
  type DBIOStream[A] = StreamingDBIO[Seq[A], A]
}