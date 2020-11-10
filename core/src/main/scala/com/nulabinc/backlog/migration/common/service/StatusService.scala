package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{BacklogCustomStatus, BacklogStatuses}

/**
 * @author uchida
 */
trait StatusService {

  def allStatuses(): BacklogStatuses

  def add(status: BacklogCustomStatus): BacklogCustomStatus

  def updateOrder(ids: Seq[Int]): Unit

  def remove(id: Int): Unit

}
