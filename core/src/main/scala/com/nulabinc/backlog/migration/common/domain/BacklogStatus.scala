package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog4j.{Issue, Status}

case class BacklogStatuses(private val values: Seq[BacklogStatus]) {

  val availableStatusNames: Seq[BacklogStatusName] = values.map(_.name)

  def findByName(name: BacklogStatusName): Option[BacklogStatus] =
    values.find(_.name == name)

  def map[B](f: BacklogStatus => B): Seq[B] =
    values.map(f)

}

case class BacklogStatusName(private val value: String) {
  val trimmed: String = value.trim
}

sealed trait BacklogStatus {
  val id: Int
  val name: BacklogStatusName
  val displayOrder: Int
}

case class BacklogDefaultStatus(
  id: Int,
  name: BacklogStatusName,
  displayOrder: Int,
) extends BacklogStatus

case class BacklogCustomStatus(
  id: Int,
  name: BacklogStatusName,
  displayOrder: Int,
  color: String
) extends BacklogStatus

object BacklogStatus {
  def from(status: Status): BacklogStatus =
    if (status.getStatusType == Issue.StatusType.Custom)
      BacklogCustomStatus(
        id = status.getId,
        name = BacklogStatusName(status.getName),
        displayOrder = status.getDisplayOrder,
        color = status.getColor.getStrValue
      )
    else
      BacklogDefaultStatus(
        id = status.getId,
        name = BacklogStatusName(status.getName),
        displayOrder = status.getDisplayOrder
      )
}