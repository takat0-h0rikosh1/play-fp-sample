import IOContextSupport.DBTask
import cats.data.Kleisli
import domain.{IOContextManager, Word, WordRepository}
import monix.eval.Task
import scalikejdbc.{AutoSession, DB, DBSession}

package object IOContextSupport {
  type DBTask[A] = Kleisli[Task, DBSession, A]
}

class IOContextManagerOnJDBC extends IOContextManager[Task, DBSession] {

  override def context: DBSession = AutoSession

  override def transactionalContext[T](
      execution: DBSession => Task[T]
  ): Task[T] =
    Task.deferFutureAction { implicit scheduler =>
      DB.futureLocalTx { session =>
        execution(session).runToFuture
      }
    }
}

class WordRepositoryOnJDBC extends WordRepository[DBTask] {
  override def resolveAll: DBTask[Seq[Word]] = Kleisli { _: DBSession =>
    Task.pure(Seq(Word("apple")))
  }
}
