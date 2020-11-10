package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogUser

/**
 * @author uchida
 */
trait ProjectUserService {

  def allProjectUsers(projectId: Long): Seq[BacklogUser]

  def add(userId: Long): Unit

}
