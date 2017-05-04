package com.nulabinc.backlog.migration.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.convert.Convert
import com.nulabinc.backlog.migration.convert.writes.UserWrites
import com.nulabinc.backlog.migration.domain.BacklogUser
import com.nulabinc.backlog4j.BacklogClient

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class UserServiceImpl @Inject()(implicit val userWrites: UserWrites, backlog: BacklogClient) extends UserService {

  override def allUsers(): Seq[BacklogUser] =
    backlog.getUsers.asScala.map(Convert.toBacklog(_))

  override def myself(): BacklogUser =
    Convert.toBacklog(backlog.getMyself)

}
