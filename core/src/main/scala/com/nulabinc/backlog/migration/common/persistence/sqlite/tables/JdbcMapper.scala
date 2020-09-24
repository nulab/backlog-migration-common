package com.nulabinc.backlog.migration.common.persistence.sqlite.tables

import java.time.{Instant, ZoneId, ZonedDateTime}

import com.nulabinc.backlog.migration.common.domain.Types.DateTime
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.SQLiteProfile.api._

object JdbcMapper {

  implicit val zonedDateTimeMapper: JdbcType[DateTime] with BaseTypedType[DateTime] =
    MappedColumnType.base[DateTime, Long](
      zonedDateTime => zonedDateTime.toInstant.getEpochSecond,
      epoch => ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault())
    )
}
