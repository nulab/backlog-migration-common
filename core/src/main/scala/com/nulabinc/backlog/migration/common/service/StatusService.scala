package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog4j.Status

/**
  * @author uchida
  */
trait StatusService {

  def allStatuses(): Seq[Status]

}
