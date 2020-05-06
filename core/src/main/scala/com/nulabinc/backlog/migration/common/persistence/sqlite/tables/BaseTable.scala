package com.nulabinc.backlog.migration.common.persistence.sqlite.tables

import com.nulabinc.backlog.migration.common.domain.Entity
import com.nulabinc.backlog.migration.common.domain.Types._
import slick.jdbc.SQLiteProfile.api._

private[sqlite] abstract class BaseTable[A <: Entity](tag: Tag, name: String)
    extends Table[A](tag, name) {

  def id: Rep[AnyId] = column[AnyId]("id", O.PrimaryKey, O.Unique, O.AutoInc)

}
