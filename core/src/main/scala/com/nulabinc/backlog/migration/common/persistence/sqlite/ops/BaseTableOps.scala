package com.nulabinc.backlog.migration.common.persistence.sqlite.ops

import com.nulabinc.backlog.migration.common.domain.{Entity, Id}
import com.nulabinc.backlog.migration.common.persistence.sqlite.DBIOTypes._
import com.nulabinc.backlog.migration.common.persistence.sqlite.{Insert, Update, WriteType}
import com.nulabinc.backlog.migration.common.persistence.sqlite.tables.BaseTable
import slick.lifted.TableQuery
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.ExecutionContext

trait BaseTableOps[A <: Entity, Table <: BaseTable[A]] {

  protected val tableQuery: TableQuery[Table]

  lazy val createTable = tableQuery.schema.create

  lazy val stream: DBIOStream[A] =
    tableQuery.result

  def select(id: Id[A]): DBIORead[Option[A]] =
    tableQuery.filter(_.id === id.value).result.headOption

  def write(obj: A, writeType: WriteType)(implicit exc: ExecutionContext): DBIOWrite =
    writeType match {
      case Insert =>
        (tableQuery returning tableQuery.map(_.id)) += obj
      case Update =>
        tableQuery.filter(_.id === obj.id).update(obj)
          .map(_ => obj.id)
    }

  def write(objs: Seq[A], writeType: WriteType)(implicit exc: ExecutionContext): DBIOWrites =
    DBIO.sequence(
      objs.map(write(_, writeType))
    )
}
