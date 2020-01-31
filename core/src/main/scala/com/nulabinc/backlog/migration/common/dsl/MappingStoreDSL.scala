package com.nulabinc.backlog.migration.common.dsl

import com.nulabinc.backlog.migration.common.domain.mappings.StatusMapping
import com.nulabinc.backlog.migration.common.dsl.DBIOTypes.DBIOWrite

trait StatusMappingQuery[A] {
  def saveQuery(mapping: StatusMapping[A]): DBIOWrite
}

trait MappingStoreDSL[F[_]] {

  val storeDSL: StoreDSL[F]

  def saveStatusMapping[A](mapping: StatusMapping[A])(implicit mq: StatusMappingQuery[A]): F[Int] =
    storeDSL.write(mq.saveQuery(mapping))
}
