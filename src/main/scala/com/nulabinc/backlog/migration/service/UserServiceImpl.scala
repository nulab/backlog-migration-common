package com.nulabinc.backlog.migration.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Backlog4jConverters
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.nulabinc.backlog4j.BacklogClient

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class UserServiceImpl @Inject()(backlog: BacklogClient) extends UserService {

  override def allUsers(): Seq[BacklogUser] =
    backlog.getUsers.asScala.map(Backlog4jConverters.User.apply)

  override def myself(): BacklogUser =
    Backlog4jConverters.User(backlog.getMyself)

}
