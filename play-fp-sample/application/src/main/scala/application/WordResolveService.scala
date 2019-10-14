package application

import cats.data.Kleisli
import domain.{IOContextManager, Word, WordResolveUseCase}

trait WordResolveService[F[_]] {
  def resolveAll: F[Seq[Word]]
}

class WordResolveServiceImpl[F[_], Ctx](
    workResolveUseCase: WordResolveUseCase[Kleisli[F, Ctx, ?]],
    ioContextManager: IOContextManager[F, Ctx]
) extends WordResolveService[F] {
  lazy val ctx: Ctx = ioContextManager.context
  def resolveAll: F[Seq[Word]] = workResolveUseCase.resolveAll.run(ctx)
}
