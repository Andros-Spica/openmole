package org.openmole.tool.cache

import collection.JavaConverters._

object WithInstance {

  /**
   * Get an instance of an object given a constructor, either pooled or new.
   * @param f
   * @param pooled
   * @param close
   * @tparam T
   * @return
   */
  def apply[T](f: () ⇒ T, pooled: Boolean, close: T ⇒ Unit = (_: T) ⇒ {}): WithInstance[T] =
    if (pooled) Pool(f, close) else WithNewInstance(f, close)
}

trait WithInstance[T] {
  def apply[A](f: T ⇒ A): A
}

object Pool {
  def apply[T](f: () ⇒ T, close: T ⇒ Unit = (_: T) ⇒ {}): Pool[T] = new Pool(f, close)
}

/**
 * A Pool of objects, given a constructor and a closing operator. A [[java.util.Stack]] of instances is maintained,
 * on which operations are done concurrently
 *
 * @param f
 * @param closeOp
 */
class Pool[T](f: () ⇒ T, closeOp: T ⇒ Unit) extends WithInstance[T] {

  val instances: java.util.Stack[T] = new java.util.Stack()

  def borrow: T = synchronized {
    instances.isEmpty match {
      case false ⇒ instances.pop()
      case true  ⇒ f()
    }
  }

  def release(t: T) = synchronized { instances.push(t) }

  def apply[A](f: T ⇒ A): A = {
    val o = borrow
    try f(o)
    finally release(o)
  }

  def close() = synchronized { instances.asScala.foreach(closeOp) }

}

case class WithNewInstance[T](o: () ⇒ T, close: T ⇒ Unit = (_: T) ⇒ {}) extends WithInstance[T] {
  def apply[A](f: T ⇒ A): A = {
    val instance = o()
    try f(instance)
    finally close(instance)
  }
}

