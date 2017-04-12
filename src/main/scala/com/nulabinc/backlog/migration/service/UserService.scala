package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogUser

/**
  * @author uchida
  */
trait UserService {

  def allUsers(): Seq[BacklogUser]

  def myself(): BacklogUser

}
