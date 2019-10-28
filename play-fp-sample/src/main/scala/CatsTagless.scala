import cats.{Applicative, Monad, MonadError, ~>}
import cats.implicits._
import cats.data.{EitherT, Kleisli}
import com.softwaremill.macwire._
import monix.eval.Task
import scalikejdbc.{AutoSession, DB, DBSession}

import scala.concurrent.Await
import scala.concurrent.duration._

object Type {
  type E[A] = EitherT[Task, Error, A]
  type R[A] = Kleisli[Task, DBSession, A]
  type RE[A] = Kleisli[E, DBSession, A]
}

import Type._

// model
sealed trait Animal
case class Cat(name: String) extends Animal
case class Dog(name: String) extends Animal

// repository
trait CatRepository[F[_]] {
  self =>

  def mapK[G[_]](nat: F ~> G): CatRepository[G] =
    new CatRepository[G] {
      override def resolveAll: G[Seq[Cat]] = nat(self.resolveAll)
    }

  def resolveAll: F[Seq[Cat]]
}

trait DogRepository[F[_]] {
  self =>

  def mapK[G[_]](nat: F ~> G): DogRepository[G] =
    new DogRepository[G] {
      override def resolveAll: G[Seq[Dog]] = nat(self.resolveAll)
    }
  def resolveAll: F[Seq[Dog]]
}

class CatRepositoryImpl extends CatRepository[R] {
  override def resolveAll: R[Seq[Cat]] = Kleisli { implicit dbSession =>
//    Task(Seq(Cat("たま")))
    Task(Seq.empty)
  }
}
class DogRepositoryImpl extends DogRepository[R] {
  override def resolveAll: R[Seq[Dog]] = Kleisli { implicit dbSession =>
//    Task(Seq(Dog("ぺろ")))
    Task(Seq.empty)
  }
}

class IOContextManagerOnJDBC extends IOContextManager[Task, DBSession] {

  override def context: DBSession = AutoSession

  override def transactionalContext[T](
      execution: (DBSession) => Task[T]
  ): Task[T] =
    Task.deferFutureAction { implicit scheduler =>
      DB.futureLocalTx { session =>
        execution(session).runToFuture
      }
    }
}

// use case
/*
trait CatResolveUseCase[F[_]] {
  def resolveAll: F[Seq[Cat]]
}
trait DogResolveUseCase[F[_]] {
  def resolveAll: F[Seq[Dog]]
}
class CatResolveUseCaseImpl[F[_]](
    catRepository: CatRepository[F]
) extends CatResolveUseCase[F] {
  def resolveAll: F[Seq[Cat]] = catRepository.resolveAll
}

class DogResolveUseCaseImpl[F[_]](
    dogRepository: DogRepository[F]
) extends DogResolveUseCase[F] {
  def resolveAll: F[Seq[Dog]] = dogRepository.resolveAll
}
 */

trait Error
case object NotFoundError extends Error
case object OtherError extends Error

// service
trait AnimalService[F[_]] {
  def resolveAll: F[Seq[Animal]]
}
class AnimalServiceImpl[F[_]: Monad](
    catRepository: CatRepository[F],
    dogRepository: DogRepository[F]
)(implicit me: MonadError[F, Error])
    extends AnimalService[F] {
  def resolveAll: F[Seq[Animal]] = {

    val result = Applicative[F].map2[Seq[Cat], Seq[Dog], Seq[Animal]](
      catRepository.resolveAll,
      dogRepository.resolveAll
    ) { (cats, dogs) =>
      cats ++ dogs
    }

    me.ensure(result)(NotFoundError)(_.nonEmpty)

/*
    result.flatMap {
      case r if r.nonEmpty => me.pure(r)
      case _               => me.raiseError(NotFoundError)
    }
*/
  }
}

trait IOContextManager[F[_], Ctx] {

  def context: Ctx

  def transactionalContext[T](execution: (Ctx) => F[T]): F[T]

}

object AnimalServiceComponent {

  lazy val ioContext: IOContextManager[Task, DBSession] =
    wire[IOContextManagerOnJDBC]

  implicit val TaskToE: Task ~> E = new (Task ~> E) {
    def apply[A](fa: Task[A]): E[A] = EitherT(fa.map(_.asRight))
  }
  implicit val RToRE: R ~> RE = new (R ~> RE) {
    def apply[A](fa: R[A]): RE[A] = fa.mapF(TaskToE(_))
  }

  lazy val catRepository: CatRepository[R] = wire[CatRepositoryImpl]
  lazy val catRepository2: CatRepository[RE] = wire[CatRepositoryImpl].mapK(RToRE)
  lazy val dogRepository: DogRepository[R] = wire[DogRepositoryImpl]
  lazy val dogRepository2: DogRepository[RE] = wire[DogRepositoryImpl].mapK(RToRE)
  /*
  lazy val catResolveUseCase: CatResolveUseCase[R] =
    wire[CatResolveUseCaseImpl[R]]
  lazy val dogResolveUseCase: DogResolveUseCase[R] =
    wire[DogResolveUseCaseImpl[R]]
   */
//  lazy val animalService: AnimalService[R] =
//    wire[AnimalServiceImpl[R]]
  lazy val animalService: AnimalService[RE] =
    wire[AnimalServiceImpl[RE]]
}

object Main {

  import monix.execution.Scheduler.Implicits.global
  import AnimalServiceComponent._

  val animals: Either[Error, Seq[Animal]] =
    Await.result(
      animalService.resolveAll.run((ioContext.context)).value.runToFuture,
      50.millisecond
    )
}
