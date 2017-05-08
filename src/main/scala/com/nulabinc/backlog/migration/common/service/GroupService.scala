package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogGroup

/**
  * @author uchida
  */
trait GroupService {

  def allGroups(): Seq[BacklogGroup]

  def create(group: BacklogGroup, propertyResolver: PropertyResolver)

}
