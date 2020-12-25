package com.nulabinc.backlog.migration.common.persistence.store.sqlite.ops

import com.nulabinc.backlog.migration.common.domain.Types.AnyId
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
    Read[(Int, Option[AnyId], String, Option[Int], Option[String], Boolean)].map {
      case (_, Some(statusId), name, Some(order), None, false) =>
        val status =
          BacklogDefaultStatus(Id.backlogStatusId(statusId), BacklogStatusName(name), order)
        ExistingExportedBacklogStatus(status)
      case (_, Some(statusId), name, Some(order), Some(color), true) =>
        val status =
          BacklogCustomStatus(Id.backlogStatusId(statusId), BacklogStatusName(name), order, color)
        ExistingExportedBacklogStatus(status)
      case (_, None, name, None, None, _) =>
        DeletedExportedBacklogStatus(BacklogStatusName(name))
      case (id, statusId, name, optDisplayOrder, optColor, isCustom) =>
        throw new IllegalStateException(
          s"Unable to find backlog status from exported_statuses table. id: $id, status_id: $statusId, name: $name, optDisplayOrder: $optDisplayOrder, optColor: $optColor, isCustom: $isCustom"
        )
    }

  implicit val write: Write[ExportedBacklogStatus] =
    Write[(Option[AnyId], String, Option[Int], Option[String], Boolean)].contramap {
      case ExistingExportedBacklogStatus(s) =>
        s match {
          case BacklogDefaultStatus(id, name, displayOrder) =>
            (Some(id.value), name.trimmed, Some(displayOrder), None, s.isCustomStatus)
          case BacklogCustomStatus(id, name, displayOrder, color) =>
            (Some(id.value), name.trimmed, Some(displayOrder), Some(color), s.isCustomStatus)
        }
      case DeletedExportedBacklogStatus(name) =>
        (None, name.trimmed, None, None, true)
    }

  def store(status: ExportedBacklogStatus): ConnectionIO[Int] = {
    import cats.implicits._

    val (selectSql, name) = status match {
      case ExistingExportedBacklogStatus(status) =>
        (sql"select id from exported_statuses where status_id = ${status.id.value}", status.name)
      case DeletedExportedBacklogStatus(name) =>
        (sql"select id from exported_statuses where name = ${name.trimmed}", name)
    }

    for {
      optId <- selectSql.query[Long].option
      result <- optId match {
        case Some(id) =>
          sql"""
            update 
              exported_statuses
            set
              name = ${name.trimmed}
            where
              id = $id
          """.update.run
        case None =>
          Update[ExportedBacklogStatus](
            """
              insert into exported_statuses
                (status_id, name, display_order, color, is_custom) 
              values 
                (?, ?, ?, ?, ?)
            """
          ).updateMany(List(status))
      }
    } yield result
  }

  def store(statuses: Seq[ExportedBacklogStatus]): ConnectionIO[Int] = {
    import cats.implicits._

    Update[ExportedBacklogStatus](
      """
        insert into exported_statuses
          (status_id, name, display_order, color, is_custom) 
        values 
          (?, ?, ?, ?, ?)
        """
    ).updateMany(statuses.toList)
  }

  def find(id: Int): Query0[ExportedBacklogStatus] =
    sql"""
      select * from exported_statuses where id = $id
    """.query[ExportedBacklogStatus]

  def getAll: Query0[ExportedBacklogStatus] =
    sql"select * from exported_statuses".query[ExportedBacklogStatus]

  def createTable(): Update0 =
    sql"""
      create table exported_statuses (
        id              integer not null primary key autoincrement,
        status_id       integer,
        name            text    not null,
        display_order   integer,
        color           text,
        is_custom       boolean not null
      )
    """.update
}
