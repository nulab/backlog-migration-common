package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.{BacklogEnvironment, BacklogSpace}

/**
  * @author uchida
  */
trait SpaceService {

  def space(): BacklogSpace

  def hasAdmin(): Boolean

  def environment(): BacklogEnvironment

}
