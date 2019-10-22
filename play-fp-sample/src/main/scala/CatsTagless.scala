import cats.Applicative
import cats.data.Kleisli
import com.softwaremill.macwire._
import monix.eval.Task

import scala.concurrent.Await
import scala.concurrent.duration._

object Type {
  type R[A] = Kleisli[Task, Unit, A]
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
  override def resolveAll: R[Seq[Cat]] = Kleisli { _: Unit =>
    Task(Seq(Cat("たま")))
  }
}
class DogRepositoryImpl extends DogRepository[R] {
  override def resolveAll: R[Seq[Dog]] = Kleisli { _: Unit =>
    Task(Seq(Dog("ぺろ")))
  }
}

// usecase
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

// service
trait AnimalService[F[_]] {
  def resolveAll: F[Seq[Animal]]
}
class AnimalServiceImpl[F[_]: Applicative](
    catResolveUseCase: CatResolveUseCase[F],
    dogResolveUseCase: DogResolveUseCase[F]
) extends AnimalService[F] {
  def resolveAll: F[Seq[Animal]] =
    Applicative[F].map2[Seq[Cat], Seq[Dog], Seq[Animal]](
      catResolveUseCase.resolveAll,
      dogResolveUseCase.resolveAll
    ) { (cats, dogs) =>
      cats ++ dogs
    }
}

object AnimalServiceComponent {

  lazy val catRepository: CatRepository[R] = wire[CatRepositoryImpl]
  lazy val dogRepository: DogRepository[R] = wire[DogRepositoryImpl]
  lazy val catResolveUseCase: CatResolveUseCase[R] =
    wire[CatResolveUseCaseImpl[R]]
  lazy val dogResolveUseCase: DogResolveUseCase[R] =
    wire[DogResolveUseCaseImpl[R]]
  lazy val animalService: AnimalService[R] =
    wire[AnimalServiceImpl[R]]
}

object Main {

  import monix.execution.Scheduler.Implicits.global
  import AnimalServiceComponent._

  val animals: Seq[Animal] =
    Await.result(animalService.resolveAll.run(()).runToFuture, 50.millisecond)
}
