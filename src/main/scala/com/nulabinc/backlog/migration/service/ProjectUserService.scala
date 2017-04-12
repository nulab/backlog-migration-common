package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogUser

/**
  * @author uchida
  */
trait ProjectUserService {

  def allProjectUsers(projectId: Long): Seq[BacklogUser]

  def add(userId: Long)

}
