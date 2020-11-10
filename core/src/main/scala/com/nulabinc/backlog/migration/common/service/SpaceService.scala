package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogSpace

/**
 * @author uchida
 */
trait SpaceService {

  def space(): BacklogSpace

  def hasAdmin(): Boolean

}
