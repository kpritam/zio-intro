package com.kpritam

import zio.ZEnv
import zio.console._
import zio.ZIO
import zio.duration.Duration
import java.util.concurrent.TimeUnit

object Failure extends zio.App {

  val failed =
    putStrLn("I am about to fail") *>
      ZIO.fail("boom!") *>
      putStrLn("Never gonna execute")

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    (failed as 0).catchAllCause { cause =>
      putStrLn(cause.prettyPrint) *> ZIO.succeed(1)
    }

}

object Interrupt extends zio.App {

  def toDouble(d: String) =
    try Right(d.toDouble)
    catch { case e: NumberFormatException => Left(e) }

  def toDuration(d: Double) = Duration(d.toLong, TimeUnit.MILLISECONDS)

  lazy val alarmDuration =
    for {
      _        <- putStrLn("Enter duration to wait: ")
      duration <- getStrLn
      d        <- ZIO.fromEither(toDouble(duration))
      fiber    <- (ZIO.sleep(Duration(500, TimeUnit.MILLISECONDS)) *> putStr(".")).forever.fork
      _        <- ZIO.sleep(toDuration(d))
      _        <- putStrLn("Woke up...")
      _        <- fiber.interrupt
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    alarmDuration.fold(_ => 1, _ => 0)

}

object StmDiningPhilosophers extends zio.App {
  import zio.console._
  import zio.stm._

  final case class Fork(id: Int)
  final case class Placement(left: TRef[Option[Fork]], right: TRef[Option[Fork]])
  final case class Roundtable(placements: Vector[Placement])

  def takeForks(left: TRef[Option[Fork]], right: TRef[Option[Fork]]): STM[Nothing, (Fork, Fork)] =
    left.get.collect { case Some(fork) => fork } zip right.get.collect { case Some(fork) => fork }

  def putForks(left: TRef[Option[Fork]], right: TRef[Option[Fork]])(tuple: (Fork, Fork)): STM[Nothing, Unit] = {
    val (leftFork, rightFork) = tuple

    for {
      _ <- left.set(Some(leftFork))
      _ <- right.set(Some(rightFork))
    } yield ()
  }

  def setupTable(size: Int): ZIO[Any, Nothing, Roundtable] = {
    def makeFork(id: Int) = TRef.make[Option[Fork]](Some(Fork(id)))

    (for {
      allForks0  <- STM.foreach(0 to size)(makeFork)
      allForks   = allForks0 ++ List(allForks0(0))
      placements = (allForks zip allForks.drop(1)).map { case (l, r) => Placement(l, r) }
    } yield Roundtable(placements.toVector)).commit
  }

  def eat(philosopher: Int, roundtable: Roundtable): ZIO[Console, Nothing, Unit] = {
    val placement = roundtable.placements(philosopher)
    val left      = placement.left
    val right     = placement.right

    for {
      forks <- takeForks(left, right).commit
      _     <- putStrLn(s"Philosopher $philosopher eating ...")
      _     <- putForks(left, right)(forks).commit
      _     <- putStrLn(s"Philosopher $philosopher is done eating ...")
    } yield ()
  }

  def run(args: List[String]): zio.ZIO[zio.ZEnv, Nothing, Int] = {
    val count = 10

    def eaters(table: Roundtable): Iterable[ZIO[Console, Nothing, Unit]] =
      (0 to count).map(i => eat(i, table))

    for {
      table <- setupTable(count)
      fiber <- ZIO.forkAll(eaters(table))
      _     <- fiber.join
      _     <- putStrLn("All philosophers have eaten!")
    } yield 0
  }
}
