package com.nulabinc.backlog.migration.common.interpreters.persistence

import doobie._
import doobie.implicits._
import com.nulabinc.backlog.migration.common.domain.BacklogStatus
import com.nulabinc.backlog.migration.common.domain.BacklogDefaultStatus
import com.nulabinc.backlog.migration.common.domain.BacklogCustomStatus
import com.nulabinc.backlog.migration.common.domain.BacklogStatusName
import com.nulabinc.backlog.migration.common.domain.Id
import com.nulabinc.backlog.migration.common.domain.BacklogStatuses

trait BaseTableOps {
  def createTable(): Update0
}

class BacklogStatusOps extends BaseTableOps {
  implicit val read: Read[BacklogStatus] =
    Read[(Int, String, Int, Option[String], Boolean)].map {
      case (id, name, displayOrder, Some(color), is_custom) if is_custom =>
        BacklogCustomStatus(Id.backlogStatusId(id), BacklogStatusName(name), displayOrder, color)
      case (id, name, displayOrder, None, is_custom) if !is_custom =>
        BacklogDefaultStatus(Id.backlogStatusId(id), BacklogStatusName(name), displayOrder)
      case (id, name, _, optColor, isCustom) =>
        throw new IllegalStateException(
          s"Cannot find backlog status. id: $id, name: $name, optColor: $optColor, isCustom: $isCustom"
        )
    }

  implicit val write: Write[BacklogStatus] =
    Write[(Long, String, Int, Option[String], Boolean)].contramap { s =>
      (s.id.value, s.name.trimmed, s.displayOrder, s.optColor, s.isCustomStatus)
    }

  def store(status: BacklogStatus): Update0 =
    sql"""
      insert into backlog_statuses
        (id, name, display_order, color, is_custom) 
      values 
        (${status.id}, ${status.name.trimmed}, ${status.displayOrder}, ${status.optColor}, ${status.isCustomStatus})
      on conflict(id) 
      do update set 
        name = ${status.name.trimmed},
        display_order = ${status.displayOrder},
        color = ${status.optColor},
        is_custom = ${status.isCustomStatus}
    """.update

  def store(statuses: BacklogStatuses): ConnectionIO[Int] = {
    import cats.implicits._

    Update[BacklogStatus](
      """
        insert into backlog_statuses
          (id, name, display_order, color, is_custom) 
        values 
          (?, ?, ?, ?, ?)
        """
    ).updateMany(statuses.values.toList)
  }

  def find(id: Int): Query0[BacklogStatus] =
    sql"""
      select 
        id, name, display_order, color, is_custom
      from
        backlog_statuses
      where
        id = $id
    """.query[BacklogStatus]

  def getAll(): Query0[BacklogStatus] =
    sql"select * from backlog_statuses".query[BacklogStatus]

  def createTable(): Update0 =
    sql"""
      create table backlog_statuses (
        id              int     not null primary key,
        name            text    not null,
        display_order   int     not null,
        color           text,
        is_custom       boolean not null
      )
    """.update
}
