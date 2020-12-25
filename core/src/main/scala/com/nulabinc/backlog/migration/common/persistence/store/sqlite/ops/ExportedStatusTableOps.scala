package com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops

import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.domain.exports.{
  DeletedExportedBacklogStatus,
  ExistingExportedBacklogStatus,
  ExportedBacklogStatus
}
import doobie._
import doobie.implicits._

object ExportedStatusTableOps extends BaseTableOps {
  implicit val read: Read[ExportedBacklogStatus] =
    Read[(Int, String, Option[Int], Option[String], Boolean)].map {
      case (id, name, Some(displayOrder), Some(color), is_exists) if is_exists =>
        val status =
          BacklogCustomStatus(Id.backlogStatusId(id), BacklogStatusName(name), displayOrder, color)
        ExistingExportedBacklogStatus(status)
      case (_, name, None, None, is_exists) if !is_exists =>
        DeletedExportedBacklogStatus(BacklogStatusName(name))
      case (id, name, optDisplayOrder, optColor, isCustom) =>
        throw new IllegalStateException(
          s"Cannot find backlog status. id: $id, name: $name, optDisplayOrder: $optDisplayOrder, optColor: $optColor, isCustom: $isCustom"
        )
    }

  implicit val write: Write[ExportedBacklogStatus] =
    Write[(String, Option[Int], Option[String], Boolean)].contramap {
      case ExistingExportedBacklogStatus(s) =>
        (s.name.trimmed, Some(s.displayOrder), s.optColor, s.isCustomStatus)
      case DeletedExportedBacklogStatus(name) =>
        (name.trimmed, None, None, false)
    }

  def store(status: ExportedBacklogStatus): Update0 = {
    val (name, optDisplayOrder, optColor, isExists) = status match {
      case ExistingExportedBacklogStatus(status) =>
        (status.name.trimmed, Some(status.displayOrder), status.optColor, true)
      case DeletedExportedBacklogStatus(name) =>
        (name.trimmed, None, None, false)
    }
    sql"""
      insert into exported_statuses
        (name, display_order, color, is_exists)
      values 
        ($name, $optDisplayOrder, $optColor, $isExists)
      on conflict(id) 
      do update set 
        name = $name,
        display_order = $optDisplayOrder,
        color = $optColor,
        is_exists = $isExists
    """.update
  }

  def store(statuses: Seq[ExportedBacklogStatus]): ConnectionIO[Int] = {
    import cats.implicits._

    Update[ExportedBacklogStatus](
      """
        insert into exported_statuses
          (id, name, display_order, color, is_exists) 
        values 
          (?, ?, ?, ?, ?)
        """
    ).updateMany(statuses.toList)
  }

  def find(id: Int): Query0[ExportedBacklogStatus] =
    sql"""
      select 
        id, name, display_order, color, is_exists
      from
        exported_statuses
      where
        id = $id
    """.query[ExportedBacklogStatus]

  def getAll: Query0[ExportedBacklogStatus] =
    sql"select * from exported_statuses".query[ExportedBacklogStatus]

  def createTable(): Update0 =
    sql"""
      create table exported_statuses (
        id              int     not null primary key,
        name            text    not null,
        display_order   int,
        color           text,
        is_exists       boolean not null
      )
    """.update
}
