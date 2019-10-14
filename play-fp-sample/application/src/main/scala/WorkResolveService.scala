import cats.data.Kleisli

trait WorkResolveService[F[_]] {
  def resolveAll: F[Seq[Word]]
}

class WorkResolveServiceImpl[F[_], Ctx](
  workResolveUseCase: WordResolveUseCase[Kleisli[F, Ctx, ?]],
  ioContextManager: IOContextManager[F, Ctx]
) extends WorkResolveService[F] {
  lazy val ctx: Ctx = ioContextManager.context
  def resolveAll: F[Seq[Word]] = workResolveUseCase.resolveAll.run(ctx)
}
