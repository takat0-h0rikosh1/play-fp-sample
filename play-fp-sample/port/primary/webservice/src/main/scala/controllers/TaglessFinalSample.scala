package controllers

import cats.implicits._
import cats.{Monad, ~>}

import scala.concurrent.Future
import scala.util.Try

trait HogeRepository[F[_]] {
  self =>
  def get: F[String]
  def mapK[G[_]](nat: F ~> G): HogeRepository[G] = new HogeRepository[G] {
    def get: G[String] = nat(self.get)
  }
}

class HogeRepositoryImpl extends HogeRepository[Try] {
  override def get: Try[String] = Try("Hoge")
}

class HogeRepositoryImpl2[F[_]: Monad] extends HogeRepository[F] {
  override def get: F[String] = Monad[F].pure("Hoge")
}

object Main {

  val tryToFuture: Try ~> Future = new (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] = Future.fromTry(fa)
  }
  val tryToOption: Try ~> Option = new (Try ~> Option) {
    override def apply[A](fa: Try[A]): Option[A] = fa.toOption
  }

//  val repository: HogeRepository[Try] = new HogeRepositoryImpl()
  val repository: HogeRepository[Future] = new HogeRepositoryImpl().mapK(tryToFuture)
  val repository2 = new HogeRepositoryImpl2[Try]

  def main(args: Array[String]): Unit = {
    println(repository.get) // Future(Success(Hoge))
  }

}
