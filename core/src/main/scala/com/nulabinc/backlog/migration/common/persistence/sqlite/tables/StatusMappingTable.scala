package com.nulabinc.backlog.migration.common.persistence.sqlite.tables

import com.nulabinc.backlog.migration.common.domain.mappings.{BacklogStatusMappingItem, MappingValue, StatusMapping}
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{Rep, Tag}

abstract class StatusMappingTable[A <: StatusMapping[B], B](tag: Tag) extends BaseTable[A](tag, "status_mappings") {
  import JdbcMapper._

  implicit val backlogStatusMappingItemMapper: JdbcType[BacklogStatusMappingItem] with BaseTypedType[BacklogStatusMappingItem] =
    MappedColumnType.base[BacklogStatusMappingItem, MappingValue](
      src => src.value,
      dst => BacklogStatusMappingItem(dst)
    )

  def optDst: Rep[Option[BacklogStatusMappingItem]] = column[Option[BacklogStatusMappingItem]]("dst")

}
