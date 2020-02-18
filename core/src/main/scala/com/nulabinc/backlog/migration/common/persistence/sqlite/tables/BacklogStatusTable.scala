package com.nulabinc.backlog.migration.common.persistence.sqlite.tables

import com.nulabinc.backlog.migration.common.domain.Types.AnyId
import com.nulabinc.backlog.migration.common.domain.{BacklogStatusName, Entity}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{ProvenShape, Rep, Tag}

case class BacklogStatusRow(id: AnyId, name: BacklogStatusName, displayOrder: Int, color: String) extends Entity

class BacklogStatusTable(tag: Tag) extends BaseTable[BacklogStatusRow](tag, "backlog_statuses") {

  implicit val backlogStatusNameMapper: JdbcType[BacklogStatusName] with BaseTypedType[BacklogStatusName] =
    MappedColumnType.base[BacklogStatusName, String](
      src => src.trimmed,
      dst => BacklogStatusName(dst)
    )

  def name: Rep[BacklogStatusName] = column[BacklogStatusName]("name")
  def displayOrder: Rep[Int] = column[Int]("display_order")
  def color: Rep[String] = column[String]("color")

  override def * : ProvenShape[BacklogStatusRow] =
    (id, name, displayOrder, color) <> (BacklogStatusRow.tupled, BacklogStatusRow.unapply)
}
