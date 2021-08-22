package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomStatus,
  BacklogStatus,
  BacklogStatuses,
  Id
}

/**
 * @author
 *   uchida
 */
trait StatusService {

  def allStatuses(): BacklogStatuses

  def add(status: BacklogCustomStatus): BacklogCustomStatus

  def updateOrder(ids: Seq[Id[BacklogStatus]]): Unit

  def remove(id: Id[BacklogStatus]): Unit

}
