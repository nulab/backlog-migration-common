package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogStatuses

/**
  * @author uchida
  */
trait StatusService {

  def allStatuses(): BacklogStatuses

}
