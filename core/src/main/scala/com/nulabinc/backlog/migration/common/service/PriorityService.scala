package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog4j.Priority

/**
 * @author
 *   uchida
 */
trait PriorityService {

  def allPriorities(): Seq[Priority]

}
