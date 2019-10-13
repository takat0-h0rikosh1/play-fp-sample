trait IOContextManager[F[_], Ctx] {

  def context: Ctx

  def transactionalContext[T](execution: Ctx => F[T]): F[T]

}

case class Word(v: String)

trait WordRepository[F[_]] {

  def resolveAll: F[Seq[Word]]

}

trait WordResolveUseCase[F[_]] {

  def resolveAll: F[Seq[Word]]
}

class WordResolveUseCaseImpl[F[_]](repository: WordRepository[F]) {

  def resolveAll: F[Seq[Word]] = repository.resolveAll
}
