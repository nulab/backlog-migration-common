package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog4j.Resolution

/**
  * @author uchida
  */
trait ResolutionService {

  def allResolutions(): Seq[Resolution]

}
