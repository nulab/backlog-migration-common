package com.nulabinc.backlog.migration.common.domain

import com.nulabinc.backlog4j.Project.CustomStatusColor
import com.nulabinc.backlog4j.{Issue, Status}

case class BacklogStatuses(val values: Seq[BacklogStatus]) {

  val availableStatusNames: Seq[BacklogStatusName] = values.map(_.name)

  def findByName(name: BacklogStatusName): Option[BacklogStatus] =
    values.find(_.name == name)

  def map[B](f: BacklogStatus => B): Seq[B] =
    values.map(f)

  def sortBy[B](
      f: BacklogStatus => B
  )(implicit ord: Ordering[B]): BacklogStatuses =
    BacklogStatuses(values.sortBy(f))

  def append(right: Seq[BacklogStatus]): BacklogStatuses =
    BacklogStatuses(values ++ right)

  def isCustomStatus(status: BacklogStatus): Boolean =
    status match {
      case _: BacklogDefaultStatus => false
      case _: BacklogCustomStatus  => true
    }

  def existsByName(name: BacklogStatusName): Boolean =
    findByName(name).nonEmpty

  def notExistByName(name: BacklogStatusName): Boolean =
    findByName(name).isEmpty
}

case class BacklogStatusName(private val value: String) {
  val trimmed: String = value.trim
}

sealed trait BacklogStatus {
  val id: Id[BacklogStatus]
  val name: BacklogStatusName
  val displayOrder: Int

  lazy val optColor: Option[String] =
    this match {
      case s: BacklogCustomStatus  => Some(s.color)
      case _: BacklogDefaultStatus => None
    }

  def isCustomStatus: Boolean =
    this match {
      case _: BacklogCustomStatus  => true
      case _: BacklogDefaultStatus => false
    }
}

case class BacklogDefaultStatus(
    id: Id[BacklogStatus],
    name: BacklogStatusName,
    displayOrder: Int
) extends BacklogStatus

case class BacklogCustomStatus(
    id: Id[BacklogStatus],
    name: BacklogStatusName,
    displayOrder: Int,
    color: String
) extends BacklogStatus

object BacklogCustomStatus {

  def from(status: Status): BacklogCustomStatus =
    BacklogCustomStatus(
      id = Id[BacklogStatus](status.getId.toLong),
      name = BacklogStatusName(status.getName),
      displayOrder = status.getDisplayOrder,
      color = status.getColor.getStrValue
    )

  def create(name: BacklogStatusName): BacklogCustomStatus =
    BacklogCustomStatus(
      id = Id[BacklogStatus](Int.MinValue),
      name = name,
      displayOrder = 3999, // undefined custom status order must be before [Closed]
      color = CustomStatusColor.Color1.getStrValue
    )

}

object BacklogStatus {
  def from(status: Status): BacklogStatus =
    if (status.getStatusType == Issue.StatusType.Custom)
      BacklogCustomStatus.from(status)
    else
      BacklogDefaultStatus(
        id = Id[BacklogStatus](status.getId.toLong),
        name = BacklogStatusName(status.getName),
        displayOrder = status.getDisplayOrder
      )
}
