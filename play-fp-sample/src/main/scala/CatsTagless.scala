import cats.Applicative
import cats.data.Kleisli
import com.softwaremill.macwire._
import monix.eval.Task
import scalikejdbc.{AutoSession, DB, DBSession}

import scala.concurrent.Await
import scala.concurrent.duration._

object Type {
  type R[A] = Kleisli[Task, DBSession, A]
}

import Type._

// model
sealed trait Animal
case class Cat(name: String) extends Animal
case class Dog(name: String) extends Animal

// repository
trait CatRepository[F[_]] {
  def resolveAll: F[Seq[Cat]]
}

trait DogRepository[F[_]] {
  def resolveAll: F[Seq[Dog]]
}

class CatRepositoryImpl extends CatRepository[R] {
  override def resolveAll: R[Seq[Cat]] = Kleisli { implicit dbSession =>
    Task(Seq(Cat("たま")))
  }
}
class DogRepositoryImpl extends DogRepository[R] {
  override def resolveAll: R[Seq[Dog]] = Kleisli { implicit dbSession =>
    Task(Seq(Dog("ぺろ")))
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

// service
trait AnimalService[F[_]] {
  def resolveAll: F[Seq[Animal]]
}
class AnimalServiceImpl[F[_]: Applicative](
    catRepository: CatRepository[F],
    dogRepository: DogRepository[F]
) extends AnimalService[F] {
  def resolveAll: F[Seq[Animal]] =
    Applicative[F].map2[Seq[Cat], Seq[Dog], Seq[Animal]](
      catRepository.resolveAll,
      dogRepository.resolveAll
    ) { (cats, dogs) =>
      cats ++ dogs
    }
}

trait IOContextManager[F[_], Ctx] {

  def context: Ctx

  def transactionalContext[T](execution: (Ctx) => F[T]): F[T]

}

object AnimalServiceComponent {

  lazy val ioContext: IOContextManager[Task, DBSession] =
    wire[IOContextManagerOnJDBC]
  lazy val catRepository: CatRepository[R] = wire[CatRepositoryImpl]
  lazy val dogRepository: DogRepository[R] = wire[DogRepositoryImpl]
  /*
  lazy val catResolveUseCase: CatResolveUseCase[R] =
    wire[CatResolveUseCaseImpl[R]]
  lazy val dogResolveUseCase: DogResolveUseCase[R] =
    wire[DogResolveUseCaseImpl[R]]
   */
  lazy val animalService: AnimalService[R] =
    wire[AnimalServiceImpl[R]]
}

object Main {

  import monix.execution.Scheduler.Implicits.global
  import AnimalServiceComponent._

  val animals: Seq[Animal] =
    Await.result(
      animalService.resolveAll.run((AutoSession)).runToFuture,
      50.millisecond
    )
}
