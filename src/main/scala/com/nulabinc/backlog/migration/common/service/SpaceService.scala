package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{BacklogEnvironment, BacklogSpace}

/**
  * @author uchida
  */
trait SpaceService {

  def space(): BacklogSpace

  def hasAdmin(): Boolean

  def environment(): BacklogEnvironment

}
