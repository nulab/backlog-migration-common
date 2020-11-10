package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog4j.Resolution

/**
 * @author uchida
 */
trait ResolutionService {

  def allResolutions(): Seq[Resolution]

}
