package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog4j.Priority

/**
  * @author uchida
  */
trait PriorityService {

  def allPriorities(): Seq[Priority]

}
