package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogGroup

/**
  * @author uchida
  */
trait GroupService {

  def allGroups(): Seq[BacklogGroup]

  def create(group: BacklogGroup, propertyResolver: PropertyResolver)

}
