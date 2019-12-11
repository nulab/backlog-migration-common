package com.nulabinc.backlog.migration.common.domain.support

trait Identifier[+A] {

  def value: A

  def getOrElse[B >: A](default: => B): B =
    if (isUndefined) default else this.value

  val isUndefined: Boolean = false

  val isDefined: Boolean = !isUndefined

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: Identifier[_] =>
      value == that.value
    case _ => false
  }

  override def hashCode = 31 * value.##

  override def toString: String = value.toString
}

trait Undefined extends Identifier[Nothing] {

  override def value = throw new NoSuchElementException("Undefined.value")

  override val isUndefined: Boolean = true

  override def equals(obj: scala.Any): Boolean = false

  override def hashCode = 0

  override def toString: String = "Undefined"

}
